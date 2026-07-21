-- First normalize the persisted association state.  A historical association
-- is a closed, valid record, never a way to leave an active invariant broken.
UPDATE integrate_subject
SET ended_at = COALESCE(ended_at, CURRENT_DATE)
WHERE state = 'historical';

UPDATE integrate_subject
SET ended_at = NULL
WHERE state = 'active';

-- Every subject must retain an active initial course.  When an older database
-- points its initial course at an association that has already been closed,
-- promote the first remaining active association deterministically.
UPDATE subject subject_row
SET initial_course_id = (
    SELECT association_row.id_course
    FROM integrate_subject association_row
    WHERE association_row.id_subject = subject_row.id_subject
      AND association_row.state = 'active'
    ORDER BY association_row.curricular_year, association_row.term, association_row.id_course
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1
    FROM integrate_subject association_row
    WHERE association_row.id_subject = subject_row.id_subject
      AND association_row.state = 'active'
)
  AND NOT EXISTS (
    SELECT 1
    FROM integrate_subject association_row
    WHERE association_row.id_subject = subject_row.id_subject
      AND association_row.id_course = subject_row.initial_course_id
      AND association_row.state = 'active'
);

-- All pre-existing class groups need at least their direct course context.
INSERT IGNORE INTO class_group_course_occurrence (
    id_class_group, id_course, id_course_occurrence, id_course_occurrence_period
)
SELECT id_class_group, id_course, id_course_occurrence, id_course_occurrence_period
FROM class_group;

-- Backfill every equivalent active course occurrence for existing class groups.
-- A class group whose own association is active must be represented in every
-- active course that integrates its subject.
INSERT IGNORE INTO class_group_course_occurrence (
    id_class_group, id_course, id_course_occurrence, id_course_occurrence_period
)
SELECT
    class_group.id_class_group,
    target_association.id_course,
    target_occurrence.id_course_occurrence,
    target_period.id_course_occurrence_period
FROM class_group
JOIN integrate_subject source_association
  ON source_association.id_course = class_group.id_course
 AND source_association.id_subject = class_group.id_subject
 AND source_association.state = 'active'
JOIN course_occurrence source_occurrence
  ON source_occurrence.id_course_occurrence = class_group.id_course_occurrence
JOIN course_occurrence_period source_period
  ON source_period.id_course_occurrence_period = class_group.id_course_occurrence_period
JOIN integrate_subject target_association
  ON target_association.id_subject = class_group.id_subject
 AND target_association.state = 'active'
JOIN course_occurrence target_occurrence
  ON target_occurrence.id_course = target_association.id_course
 AND target_occurrence.reference_year = source_occurrence.reference_year
 AND target_occurrence.starts_at = source_occurrence.starts_at
 AND target_occurrence.ends_at = source_occurrence.ends_at
 AND target_occurrence.state = source_occurrence.state
JOIN course_occurrence_period target_period
  ON target_period.id_course_occurrence = target_occurrence.id_course_occurrence
 AND target_period.curricular_year = target_association.curricular_year
 AND target_period.term = target_association.term
 AND target_period.starts_at = source_period.starts_at
 AND target_period.ends_at = source_period.ends_at
 AND target_period.state = source_period.state;

-- If legacy data cannot be shared because the occurrences or periods are not
-- equivalent, retain one active association (the subject's initial course)
-- and close the incompatible active associations.  The class-group and
-- association records are preserved, but the resulting database has no
-- invalid active relationship.
CREATE TEMPORARY TABLE gape_invalid_shared_class_group_subject (
    id_subject BIGINT PRIMARY KEY
)
SELECT DISTINCT class_group_row.id_subject
FROM class_group class_group_row
JOIN integrate_subject source_association
  ON source_association.id_course = class_group_row.id_course
 AND source_association.id_subject = class_group_row.id_subject
 AND source_association.state = 'active'
WHERE (
    SELECT COUNT(*)
    FROM integrate_subject active_association
    WHERE active_association.id_subject = class_group_row.id_subject
      AND active_association.state = 'active'
) <> (
    SELECT COUNT(DISTINCT shared_context.id_course)
    FROM class_group_course_occurrence shared_context
    WHERE shared_context.id_class_group = class_group_row.id_class_group
);

UPDATE integrate_subject association_row
JOIN gape_invalid_shared_class_group_subject invalid_subject
  ON invalid_subject.id_subject = association_row.id_subject
JOIN subject subject_row
  ON subject_row.id_subject = association_row.id_subject
SET association_row.state = 'historical',
    association_row.ended_at = CURRENT_DATE
WHERE association_row.state = 'active'
  AND association_row.id_course <> subject_row.initial_course_id;

-- Contexts belonging to a now-closed association cannot remain as active
-- contexts.  Removing them keeps the context count exact for the surviving
-- active association while retaining the class-group record itself.
DELETE shared_context
FROM class_group_course_occurrence shared_context
JOIN class_group class_group_row
  ON class_group_row.id_class_group = shared_context.id_class_group
JOIN integrate_subject source_association
  ON source_association.id_course = class_group_row.id_course
 AND source_association.id_subject = class_group_row.id_subject
 AND source_association.state = 'active'
LEFT JOIN integrate_subject target_association
  ON target_association.id_course = shared_context.id_course
 AND target_association.id_subject = class_group_row.id_subject
 AND target_association.state = 'active'
WHERE target_association.id_course IS NULL;

DELIMITER $$
DROP PROCEDURE IF EXISTS gape_assert_shared_class_group_contexts$$
CREATE PROCEDURE gape_assert_shared_class_group_contexts()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM subject subject_row
        WHERE NOT EXISTS (
            SELECT 1
            FROM integrate_subject association_row
            WHERE association_row.id_subject = subject_row.id_subject
              AND association_row.state = 'active'
        )
           OR NOT EXISTS (
            SELECT 1
            FROM integrate_subject association_row
            WHERE association_row.id_subject = subject_row.id_subject
              AND association_row.id_course = subject_row.initial_course_id
              AND association_row.state = 'active'
        )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Every subject must have an active initial course association';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM class_group class_group_row
        JOIN integrate_subject source_association
          ON source_association.id_course = class_group_row.id_course
         AND source_association.id_subject = class_group_row.id_subject
         AND source_association.state = 'active'
        WHERE (
            SELECT COUNT(*)
            FROM integrate_subject active_association
            WHERE active_association.id_subject = class_group_row.id_subject
              AND active_association.state = 'active'
        ) <> (
            SELECT COUNT(DISTINCT shared_context.id_course)
            FROM class_group_course_occurrence shared_context
            WHERE shared_context.id_class_group = class_group_row.id_class_group
        )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Every active class group must have an equivalent context in every active course of its subject';
    END IF;
END$$
CALL gape_assert_shared_class_group_contexts()$$
DROP PROCEDURE gape_assert_shared_class_group_contexts$$
DELIMITER ;

DROP TEMPORARY TABLE gape_invalid_shared_class_group_subject;
