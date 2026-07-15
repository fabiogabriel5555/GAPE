-- Preserve the course selected when a subject is created. Existing subjects
-- are initialized from their first current association in the displayed order.
SET @gape_has_subject_initial_course = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'subject'
      AND column_name = 'initial_course_id'
);
SET @gape_subject_initial_course_sql = IF(
    @gape_has_subject_initial_course = 0,
    'ALTER TABLE subject ADD COLUMN initial_course_id BIGINT UNSIGNED NULL AFTER id_organization',
    'SELECT 1'
);
PREPARE gape_subject_initial_course_statement FROM @gape_subject_initial_course_sql;
EXECUTE gape_subject_initial_course_statement;
DEALLOCATE PREPARE gape_subject_initial_course_statement;

UPDATE subject s
SET initial_course_id = (
    SELECT association_row.id_course
    FROM integrate_subject association_row
    WHERE association_row.id_subject = s.id_subject
    ORDER BY association_row.curricular_year, association_row.term, association_row.id_course
    LIMIT 1
)
WHERE s.initial_course_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM integrate_subject association_row
      WHERE association_row.id_subject = s.id_subject
  );

DELIMITER $$
DROP PROCEDURE IF EXISTS gape_assert_subject_initial_course$$
CREATE PROCEDURE gape_assert_subject_initial_course()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM subject
        WHERE initial_course_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Every subject must have an associated initial course before migration V013 can complete';
    END IF;
END$$
CALL gape_assert_subject_initial_course()$$
DROP PROCEDURE gape_assert_subject_initial_course$$
DELIMITER ;

SET @gape_has_subject_initial_course_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'subject'
      AND index_name = 'idx_subject_initial_course'
);
SET @gape_subject_initial_course_index_sql = IF(
    @gape_has_subject_initial_course_index = 0,
    'ALTER TABLE subject ADD KEY idx_subject_initial_course (initial_course_id)',
    'SELECT 1'
);
PREPARE gape_subject_initial_course_index_statement FROM @gape_subject_initial_course_index_sql;
EXECUTE gape_subject_initial_course_index_statement;
DEALLOCATE PREPARE gape_subject_initial_course_index_statement;

SET @gape_has_subject_initial_course_fk = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'subject'
      AND constraint_name = 'fk_subject_initial_course'
      AND constraint_type = 'FOREIGN KEY'
);
SET @gape_subject_initial_course_fk_sql = IF(
    @gape_has_subject_initial_course_fk = 0,
    'ALTER TABLE subject ADD CONSTRAINT fk_subject_initial_course FOREIGN KEY (initial_course_id) REFERENCES course (id_course) ON UPDATE CASCADE ON DELETE RESTRICT',
    'SELECT 1'
);
PREPARE gape_subject_initial_course_fk_statement FROM @gape_subject_initial_course_fk_sql;
EXECUTE gape_subject_initial_course_fk_statement;
DEALLOCATE PREPARE gape_subject_initial_course_fk_statement;

DELIMITER $$
DROP TRIGGER IF EXISTS bi_integrate_subject_validate$$
CREATE TRIGGER bi_integrate_subject_validate
BEFORE INSERT ON integrate_subject
FOR EACH ROW
BEGIN
    DECLARE v_course_org BIGINT UNSIGNED;
    DECLARE v_subject_org BIGINT UNSIGNED;
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT id_organization, state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_org, v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_organization
    INTO v_subject_org
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_org <> v_subject_org THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course and Subject must belong to the same Organization';
    END IF;

    IF v_course_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires an active Course';
    END IF;

    IF NEW.curricular_year IS NULL OR NEW.term IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires course year and period';
    END IF;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_course_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject course year must stay inside Course duration';
    END IF;

    IF (v_course_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_course_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_course_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_course_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_course_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_course_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject term must match Course frequency';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_integrate_subject_validate$$
CREATE TRIGGER bu_integrate_subject_validate
BEFORE UPDATE ON integrate_subject
FOR EACH ROW
BEGIN
    DECLARE v_course_org BIGINT UNSIGNED;
    DECLARE v_subject_org BIGINT UNSIGNED;
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT id_organization, state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_org, v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_organization
    INTO v_subject_org
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_org <> v_subject_org THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course and Subject must belong to the same Organization';
    END IF;

    IF v_course_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires an active Course';
    END IF;

    IF NEW.curricular_year IS NULL OR NEW.term IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires course year and period';
    END IF;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_course_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject course year must stay inside Course duration';
    END IF;

    IF (v_course_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_course_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_course_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_course_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_course_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_course_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject term must match Course frequency';
    END IF;
END$$
DELIMITER ;
