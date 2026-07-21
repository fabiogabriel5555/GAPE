-- A Subject has its own optional Organic Unit context. That context is
-- independent from the Course contexts where the Subject may be associated.
--
-- sql/schema.sql is deliberately the current schema, so a fresh database
-- already has this column, index and foreign key before pending migrations
-- are recorded. Guard each DDL change so the migration also remains valid for
-- that bootstrap path while still upgrading a version-27 database.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_subject_organic_unit_context$$
CREATE PROCEDURE migrate_subject_organic_unit_context()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'subject'
          AND column_name = 'id_organic_unit'
    ) THEN
        ALTER TABLE subject
            ADD COLUMN id_organic_unit BIGINT UNSIGNED NULL AFTER id_organization;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'subject'
          AND index_name = 'idx_subject_organic_unit'
    ) THEN
        ALTER TABLE subject
            ADD INDEX idx_subject_organic_unit (id_organic_unit);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'subject'
          AND constraint_name = 'fk_subject_organic_unit'
    ) THEN
        ALTER TABLE subject
            ADD CONSTRAINT fk_subject_organic_unit
                FOREIGN KEY (id_organic_unit) REFERENCES organic_unit (id_organic_unit)
                ON UPDATE CASCADE ON DELETE RESTRICT;
    END IF;
END$$
CALL migrate_subject_organic_unit_context()$$
DROP PROCEDURE migrate_subject_organic_unit_context$$
DELIMITER ;

DELIMITER $$
DROP TRIGGER IF EXISTS bi_subject_validate$$
CREATE TRIGGER bi_subject_validate
BEFORE INSERT ON subject
FOR EACH ROW
BEGIN
    DECLARE v_organization_state VARCHAR(20);
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    SELECT state
    INTO v_organization_state
    FROM organization
    WHERE id_organization = NEW.id_organization;

    IF NEW.state = 'active' AND v_organization_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject cannot be active in inactive Organization';
    END IF;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_subject_validate$$
CREATE TRIGGER bu_subject_validate
BEFORE UPDATE ON subject
FOR EACH ROW
BEGIN
    DECLARE v_organization_state VARCHAR(20);
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    SELECT state
    INTO v_organization_state
    FROM organization
    WHERE id_organization = NEW.id_organization;

    IF NEW.state = 'active' AND v_organization_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject cannot be active in inactive Organization';
    END IF;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_integrate_subject_validate$$
CREATE TRIGGER bi_integrate_subject_validate
BEFORE INSERT ON integrate_subject
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT state
    INTO v_subject_state
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires an active Course';
    END IF;

    IF NEW.state = 'active' AND v_subject_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Active Course_Subject association requires an active Subject';
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
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT state
    INTO v_subject_state
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires an active Course';
    END IF;

    IF NEW.state = 'active' AND v_subject_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Active Course_Subject association requires an active Subject';
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
