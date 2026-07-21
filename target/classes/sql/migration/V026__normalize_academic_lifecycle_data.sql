-- Lifecycle state is a persisted representation of a concrete time context.
-- Normalize pre-existing data before the application starts serving it, rather
-- than retaining stale state values as historical residue.
UPDATE course_occurrence
SET state = CASE
    WHEN state = 'cancelled' THEN 'cancelled'
    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
    WHEN ends_at < CURRENT_DATE THEN 'completed'
    ELSE 'active'
END
WHERE state <> CASE
    WHEN state = 'cancelled' THEN 'cancelled'
    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
    WHEN ends_at < CURRENT_DATE THEN 'completed'
    ELSE 'active'
END;

UPDATE course_occurrence_period
SET state = CASE
    WHEN state = 'cancelled' THEN 'cancelled'
    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
    WHEN ends_at < CURRENT_DATE THEN 'completed'
    ELSE 'active'
END
WHERE state <> CASE
    WHEN state = 'cancelled' THEN 'cancelled'
    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
    WHEN ends_at < CURRENT_DATE THEN 'completed'
    ELSE 'active'
END;

UPDATE class_group
SET state = CASE
    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
    WHEN ends_at < CURRENT_DATE THEN 'completed'
    ELSE 'active'
END
WHERE state <> CASE
    WHEN starts_at > CURRENT_DATE THEN 'scheduled'
    WHEN ends_at < CURRENT_DATE THEN 'completed'
    ELSE 'active'
END;

UPDATE lesson
SET state = CASE
    WHEN ends_at <= CURRENT_TIMESTAMP THEN 'completed'
    WHEN starts_at > CURRENT_TIMESTAMP THEN 'scheduled'
    ELSE 'active'
END
WHERE state IN ('scheduled', 'active')
  AND state <> CASE
    WHEN ends_at <= CURRENT_TIMESTAMP THEN 'completed'
    WHEN starts_at > CURRENT_TIMESTAMP THEN 'scheduled'
    ELSE 'active'
END;

UPDATE assessment
SET state = CASE
    WHEN available_until <= CURRENT_TIMESTAMP THEN 'completed'
    WHEN available_from > CURRENT_TIMESTAMP THEN 'scheduled'
    ELSE 'active'
END
WHERE state IN ('scheduled', 'active')
  AND state <> CASE
    WHEN available_until <= CURRENT_TIMESTAMP THEN 'completed'
    WHEN available_from > CURRENT_TIMESTAMP THEN 'scheduled'
    ELSE 'active'
END;

UPDATE enroll_course enrollment
JOIN course_occurrence occurrence
  ON occurrence.id_course_occurrence = enrollment.id_course_occurrence
SET enrollment.state = 'completed',
    enrollment.end_date = CASE
        WHEN enrollment.end_date IS NULL OR enrollment.end_date > occurrence.ends_at
            THEN occurrence.ends_at
        ELSE enrollment.end_date
    END
WHERE enrollment.state = 'active'
  AND occurrence.state = 'completed';

UPDATE enroll_class_group enrollment
JOIN class_group class_group_row
  ON class_group_row.id_class_group = enrollment.id_class_group
SET enrollment.state = 'completed',
    enrollment.end_date = CASE
        WHEN enrollment.end_date IS NULL OR enrollment.end_date > class_group_row.ends_at
            THEN class_group_row.ends_at
        ELSE enrollment.end_date
    END
WHERE enrollment.state = 'active'
  AND class_group_row.state = 'completed';

UPDATE teach_class_group teaching
JOIN class_group class_group_row
  ON class_group_row.id_class_group = teaching.id_class_group
SET teaching.state = 'inactive',
    teaching.end_date = CASE
        WHEN teaching.end_date IS NULL OR teaching.end_date > class_group_row.ends_at
            THEN class_group_row.ends_at
        ELSE teaching.end_date
    END
WHERE teaching.state = 'active'
  AND class_group_row.state = 'completed';

UPDATE enroll_assessment enrollment
JOIN assessment assessment_row
  ON assessment_row.id_assessment = enrollment.id_assessment
SET enrollment.state = 'completed',
    enrollment.end_date = CASE
        WHEN enrollment.end_date IS NULL
             OR enrollment.end_date > DATE(assessment_row.available_until)
            THEN DATE(assessment_row.available_until)
        ELSE enrollment.end_date
    END
WHERE enrollment.state = 'active'
  AND assessment_row.state = 'completed'
  AND assessment_row.available_until IS NOT NULL;

-- A class-group event is a historical roster snapshot.  Keep only people who
-- belonged to its group at the event date, then restore any missing eligible
-- student, teacher or coordinator recipient.  Events without a class-group
-- association are deliberately left untouched because their recipients are
-- authored independently.
DELETE rse
FROM receive_schedule_event rse
JOIN associate_schedule_event_class_group aseg
  ON aseg.id_schedule_event = rse.id_schedule_event
WHERE NOT EXISTS (
    SELECT 1
    FROM associate_schedule_event_class_group valid_aseg
    JOIN schedule_event event_row
      ON event_row.id_schedule_event = valid_aseg.id_schedule_event
    JOIN enroll_class_group enrollment
      ON enrollment.id_class_group = valid_aseg.id_class_group
    JOIN user_account user_row
      ON user_row.id_user = enrollment.id_student_user
    WHERE valid_aseg.id_schedule_event = rse.id_schedule_event
      AND enrollment.id_student_user = rse.id_user
      AND enrollment.state IN ('active', 'completed')
      AND user_row.state = 'active'
      AND enrollment.start_date <= DATE(event_row.starts_at)
      AND enrollment.end_date >= DATE(event_row.starts_at)
    UNION ALL
    SELECT 1
    FROM associate_schedule_event_class_group valid_aseg
    JOIN schedule_event event_row
      ON event_row.id_schedule_event = valid_aseg.id_schedule_event
    JOIN teach_class_group teaching
      ON teaching.id_class_group = valid_aseg.id_class_group
    JOIN user_account user_row
      ON user_row.id_user = teaching.id_teacher_user
    WHERE valid_aseg.id_schedule_event = rse.id_schedule_event
      AND teaching.id_teacher_user = rse.id_user
      AND teaching.state IN ('active', 'inactive')
      AND user_row.state = 'active'
      AND (teaching.start_date IS NULL OR teaching.start_date <= DATE(event_row.starts_at))
      AND (teaching.end_date IS NULL OR teaching.end_date >= DATE(event_row.starts_at))
    UNION ALL
    SELECT 1
    FROM associate_schedule_event_class_group valid_aseg
    JOIN class_group class_group_row
      ON class_group_row.id_class_group = valid_aseg.id_class_group
    JOIN coordinate_subject coordinator
      ON coordinator.id_subject = class_group_row.id_subject
    JOIN user_account user_row
      ON user_row.id_user = coordinator.id_coordinator_user
    WHERE valid_aseg.id_schedule_event = rse.id_schedule_event
      AND coordinator.id_coordinator_user = rse.id_user
      AND coordinator.state = 'active'
      AND user_row.state = 'active'
);

INSERT IGNORE INTO receive_schedule_event (id_user, id_schedule_event)
SELECT expected.id_user, expected.id_schedule_event
FROM (
    SELECT DISTINCT enrollment.id_student_user AS id_user,
           aseg.id_schedule_event
    FROM associate_schedule_event_class_group aseg
    JOIN schedule_event event_row
      ON event_row.id_schedule_event = aseg.id_schedule_event
    JOIN enroll_class_group enrollment
      ON enrollment.id_class_group = aseg.id_class_group
    JOIN user_account user_row
      ON user_row.id_user = enrollment.id_student_user
    WHERE enrollment.state IN ('active', 'completed')
      AND user_row.state = 'active'
      AND enrollment.start_date <= DATE(event_row.starts_at)
      AND enrollment.end_date >= DATE(event_row.starts_at)
    UNION
    SELECT DISTINCT teaching.id_teacher_user AS id_user,
           aseg.id_schedule_event
    FROM associate_schedule_event_class_group aseg
    JOIN schedule_event event_row
      ON event_row.id_schedule_event = aseg.id_schedule_event
    JOIN teach_class_group teaching
      ON teaching.id_class_group = aseg.id_class_group
    JOIN user_account user_row
      ON user_row.id_user = teaching.id_teacher_user
    WHERE teaching.state IN ('active', 'inactive')
      AND user_row.state = 'active'
      AND (teaching.start_date IS NULL OR teaching.start_date <= DATE(event_row.starts_at))
      AND (teaching.end_date IS NULL OR teaching.end_date >= DATE(event_row.starts_at))
    UNION
    SELECT DISTINCT coordinator.id_coordinator_user AS id_user,
           aseg.id_schedule_event
    FROM associate_schedule_event_class_group aseg
    JOIN class_group class_group_row
      ON class_group_row.id_class_group = aseg.id_class_group
    JOIN coordinate_subject coordinator
      ON coordinator.id_subject = class_group_row.id_subject
    JOIN user_account user_row
      ON user_row.id_user = coordinator.id_coordinator_user
    WHERE coordinator.state = 'active'
      AND user_row.state = 'active'
) expected;
