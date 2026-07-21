-- Every student enrollment has a persisted date range derived from its occurrence.
UPDATE enroll_course ec
JOIN course_occurrence co ON co.id_course_occurrence = ec.id_course_occurrence
SET ec.start_date = COALESCE(ec.start_date, co.starts_at),
    ec.end_date = COALESCE(ec.end_date, co.ends_at)
WHERE ec.start_date IS NULL OR ec.end_date IS NULL;

UPDATE enroll_class_group ecg
JOIN class_group cg ON cg.id_class_group = ecg.id_class_group
SET ecg.start_date = COALESCE(ecg.start_date, cg.starts_at),
    ecg.end_date = COALESCE(ecg.end_date, cg.ends_at)
WHERE ecg.start_date IS NULL OR ecg.end_date IS NULL;

SET @has_enrollment_assessment_start_date := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'enroll_assessment'
      AND column_name = 'start_date'
);
SET @enrollment_assessment_ddl := IF(
    @has_enrollment_assessment_start_date = 0,
    'ALTER TABLE enroll_assessment ADD COLUMN start_date DATE NULL AFTER state',
    'DO 0'
);
PREPARE enrollment_assessment_statement FROM @enrollment_assessment_ddl;
EXECUTE enrollment_assessment_statement;
DEALLOCATE PREPARE enrollment_assessment_statement;

SET @has_enrollment_assessment_end_date := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'enroll_assessment'
      AND column_name = 'end_date'
);
SET @enrollment_assessment_ddl := IF(
    @has_enrollment_assessment_end_date = 0,
    'ALTER TABLE enroll_assessment ADD COLUMN end_date DATE NULL AFTER start_date',
    'DO 0'
);
PREPARE enrollment_assessment_statement FROM @enrollment_assessment_ddl;
EXECUTE enrollment_assessment_statement;
DEALLOCATE PREPARE enrollment_assessment_statement;

UPDATE enroll_assessment ea
JOIN assessment a ON a.id_assessment = ea.id_assessment
SET ea.start_date = DATE(a.available_from),
    ea.end_date = DATE(a.available_until)
WHERE ea.start_date IS NULL OR ea.end_date IS NULL;

ALTER TABLE enroll_course
    MODIFY start_date DATE NOT NULL DEFAULT '1000-01-01',
    MODIFY end_date DATE NOT NULL DEFAULT '1000-01-01',
    DROP CHECK ck_enroll_course_dates,
    ADD CONSTRAINT ck_enroll_course_dates CHECK (end_date >= start_date);

ALTER TABLE enroll_class_group
    MODIFY start_date DATE NOT NULL DEFAULT '1000-01-01',
    MODIFY end_date DATE NOT NULL DEFAULT '1000-01-01',
    DROP CHECK ck_enroll_class_group_dates,
    ADD CONSTRAINT ck_enroll_class_group_dates CHECK (end_date >= start_date);

ALTER TABLE enroll_assessment
    MODIFY start_date DATE NOT NULL DEFAULT '1000-01-01',
    MODIFY end_date DATE NOT NULL DEFAULT '1000-01-01';

SET @has_enrollment_assessment_dates_check := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'enroll_assessment'
      AND constraint_name = 'ck_enroll_assessment_dates'
      AND constraint_type = 'CHECK'
);
SET @enrollment_assessment_ddl := IF(
    @has_enrollment_assessment_dates_check = 1,
    'ALTER TABLE enroll_assessment DROP CHECK ck_enroll_assessment_dates',
    'DO 0'
);
PREPARE enrollment_assessment_statement FROM @enrollment_assessment_ddl;
EXECUTE enrollment_assessment_statement;
DEALLOCATE PREPARE enrollment_assessment_statement;

ALTER TABLE enroll_assessment
    ADD CONSTRAINT ck_enroll_assessment_dates CHECK (end_date >= start_date);

DELIMITER $$

DROP TRIGGER IF EXISTS bi_enroll_course_validate$$
CREATE TRIGGER bi_enroll_course_validate
BEFORE INSERT ON enroll_course
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_student_count INT DEFAULT 0;

    SELECT state INTO v_course_state FROM course WHERE id_course = NEW.id_course;
    SELECT id_course, state, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_state, v_occurrence_start, v_occurrence_end
    FROM course_occurrence WHERE id_course_occurrence = NEW.id_course_occurrence;
    SELECT COUNT(*) INTO v_student_count
    FROM student_profile sp JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user AND u.state = 'active';

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment occurrence must belong to the selected Course';
    END IF;
    IF NEW.start_date IS NOT NULL AND NEW.start_date <> '1000-01-01'
       AND (NEW.start_date < v_occurrence_start OR NEW.start_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment start date must stay inside the Course occurrence';
    END IF;
    IF NEW.end_date IS NOT NULL AND NEW.end_date <> '1000-01-01'
       AND (NEW.end_date < v_occurrence_start OR NEW.end_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment end date must stay inside the Course occurrence';
    END IF;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_occurrence_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_occurrence_end
        ELSE NEW.end_date
    END;

    IF NEW.state = 'active' AND (
        v_course_state <> 'active'
        OR v_occurrence_state NOT IN ('scheduled', 'active')
        OR CURRENT_DATE > v_occurrence_end
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires scheduled or active Course occurrence';
    END IF;
    IF NEW.state = 'active' AND v_student_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Student';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_enroll_course_validate$$
CREATE TRIGGER bu_enroll_course_validate
BEFORE UPDATE ON enroll_course
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_student_count INT DEFAULT 0;

    SELECT state INTO v_course_state FROM course WHERE id_course = NEW.id_course;
    SELECT id_course, state, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_state, v_occurrence_start, v_occurrence_end
    FROM course_occurrence WHERE id_course_occurrence = NEW.id_course_occurrence;
    SELECT COUNT(*) INTO v_student_count
    FROM student_profile sp JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user AND u.state = 'active';

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment occurrence must belong to the selected Course';
    END IF;
    IF NEW.start_date IS NOT NULL AND NEW.start_date <> '1000-01-01'
       AND (NEW.start_date < v_occurrence_start OR NEW.start_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment start date must stay inside the Course occurrence';
    END IF;
    IF NEW.end_date IS NOT NULL AND NEW.end_date <> '1000-01-01'
       AND (NEW.end_date < v_occurrence_start OR NEW.end_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment end date must stay inside the Course occurrence';
    END IF;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_occurrence_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_occurrence_end
        ELSE NEW.end_date
    END;

    IF NEW.state = 'active' AND (
        v_course_state <> 'active'
        OR v_occurrence_state NOT IN ('scheduled', 'active')
        OR CURRENT_DATE > v_occurrence_end
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires scheduled or active Course occurrence';
    END IF;
    IF NEW.state = 'active' AND v_student_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Student';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_enroll_class_group_validate$$
CREATE TRIGGER bi_enroll_class_group_validate
BEFORE INSERT ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_subject_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_class_start DATE;
    DECLARE v_class_end DATE;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_subject, cg.id_course_occurrence, cg.starts_at, cg.ends_at,
           cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_subject_id, v_course_occurrence_id, v_class_start, v_class_end,
         v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_class_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_class_end
        ELSE NEW.end_date
    END;
    IF NEW.start_date < v_class_start OR NEW.start_date > v_class_end
       OR NEW.end_date < v_class_start OR NEW.end_date > v_class_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment dates must stay inside the Class_Group occurrence period';
    END IF;

    SELECT COUNT(*) INTO v_student_count
    FROM student_profile sp JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user AND u.state = 'active';
    SELECT COUNT(*) INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND ec.start_date <= NEW.start_date
      AND ec.end_date >= NEW.end_date;
    SELECT COUNT(*) INTO v_overlap_count
    FROM enroll_class_group ecg
    JOIN class_group existing_cg ON existing_cg.id_class_group = ecg.id_class_group
    WHERE ecg.id_student_user = NEW.id_student_user
      AND existing_cg.id_course = v_course_id
      AND existing_cg.id_subject = v_subject_id
      AND existing_cg.id_course_occurrence = v_course_occurrence_id
      AND ecg.state = 'active'
      AND ecg.start_date <= NEW.end_date
      AND ecg.end_date >= NEW.start_date;
    SELECT COUNT(*) INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group AND state = 'active';

    IF NEW.state = 'active' AND (
        v_student_count = 0 OR v_class_state <> 'active' OR v_course_state <> 'active'
        OR v_subject_state <> 'active' OR v_association_course IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group enrollment requires active Student, Class_Group, Course, Subject and course-subject association';
    END IF;
    IF NEW.state = 'active' AND v_course_enrollment_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment requires active Course occurrence enrollment for the full period';
    END IF;
    IF NEW.state = 'active' AND v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Student already has an overlapping active enrollment in this Course/Subject class group context';
    END IF;
    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_enroll_class_group_validate$$
CREATE TRIGGER bu_enroll_class_group_validate
BEFORE UPDATE ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_subject_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_class_start DATE;
    DECLARE v_class_end DATE;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_subject, cg.id_course_occurrence, cg.starts_at, cg.ends_at,
           cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_subject_id, v_course_occurrence_id, v_class_start, v_class_end,
         v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_class_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_class_end
        ELSE NEW.end_date
    END;
    IF NEW.start_date < v_class_start OR NEW.start_date > v_class_end
       OR NEW.end_date < v_class_start OR NEW.end_date > v_class_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment dates must stay inside the Class_Group occurrence period';
    END IF;

    SELECT COUNT(*) INTO v_student_count
    FROM student_profile sp JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user AND u.state = 'active';
    SELECT COUNT(*) INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND ec.start_date <= NEW.start_date
      AND ec.end_date >= NEW.end_date;
    SELECT COUNT(*) INTO v_overlap_count
    FROM enroll_class_group ecg
    JOIN class_group existing_cg ON existing_cg.id_class_group = ecg.id_class_group
    WHERE ecg.id_student_user = NEW.id_student_user
      AND existing_cg.id_course = v_course_id
      AND existing_cg.id_subject = v_subject_id
      AND existing_cg.id_course_occurrence = v_course_occurrence_id
      AND ecg.state = 'active'
      AND NOT (ecg.id_student_user = OLD.id_student_user AND ecg.id_class_group = OLD.id_class_group)
      AND ecg.start_date <= NEW.end_date
      AND ecg.end_date >= NEW.start_date;
    SELECT COUNT(*) INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group AND state = 'active'
      AND NOT (id_student_user = OLD.id_student_user AND id_class_group = OLD.id_class_group);

    IF NEW.state = 'active' AND (
        v_student_count = 0 OR v_class_state <> 'active' OR v_course_state <> 'active'
        OR v_subject_state <> 'active' OR v_association_course IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group enrollment requires active Student, Class_Group, Course, Subject and course-subject association';
    END IF;
    IF NEW.state = 'active' AND v_course_enrollment_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment requires active Course occurrence enrollment for the full period';
    END IF;
    IF NEW.state = 'active' AND v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Student already has an overlapping active enrollment in this Course/Subject class group context';
    END IF;
    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_enroll_assessment_validate$$
CREATE TRIGGER bi_enroll_assessment_validate
BEFORE INSERT ON enroll_assessment
FOR EACH ROW
BEGIN
    DECLARE v_available_start DATE;
    DECLARE v_available_end DATE;

    SELECT DATE(available_from), DATE(available_until)
    INTO v_available_start, v_available_end
    FROM assessment
    WHERE id_assessment = NEW.id_assessment;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_available_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_available_end
        ELSE NEW.end_date
    END;

    IF NEW.start_date < v_available_start OR NEW.start_date > v_available_end
       OR NEW.end_date < v_available_start OR NEW.end_date > v_available_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment enrollment dates must stay inside the assessment availability period';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_enroll_assessment_validate$$
CREATE TRIGGER bu_enroll_assessment_validate
BEFORE UPDATE ON enroll_assessment
FOR EACH ROW
BEGIN
    DECLARE v_available_start DATE;
    DECLARE v_available_end DATE;

    SELECT DATE(available_from), DATE(available_until)
    INTO v_available_start, v_available_end
    FROM assessment
    WHERE id_assessment = NEW.id_assessment;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_available_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_available_end
        ELSE NEW.end_date
    END;

    IF NEW.start_date < v_available_start OR NEW.start_date > v_available_end
       OR NEW.end_date < v_available_start OR NEW.end_date > v_available_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment enrollment dates must stay inside the assessment availability period';
    END IF;
END$$

DELIMITER ;
