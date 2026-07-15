DROP PROCEDURE IF EXISTS gape_migrate_v003;

DELIMITER $$
CREATE PROCEDURE gape_migrate_v003()
BEGIN
    DECLARE v_count BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM certificate
    WHERE LOWER(state) NOT IN ('draft', 'active', 'issued', 'revoked');

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Certificate migration stopped: unsupported legacy state';
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM certificate
    WHERE LOWER(state) IN ('issued', 'revoked')
      AND (validation_code IS NULL OR issued_at IS NULL OR final_grade IS NULL);

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Certificate migration stopped: issued/revoked rows require validation code, issue date and final grade';
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM certificate
    WHERE LOWER(state) IN ('draft', 'active')
      AND (validation_code IS NOT NULL OR issued_at IS NOT NULL OR final_grade IS NOT NULL);

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Certificate migration stopped: non-issued rows contain issue snapshot data';
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM (
        SELECT id_course_occurrence, id_user_student
        FROM certificate
        GROUP BY id_course_occurrence, id_user_student
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Certificate migration stopped: duplicate occurrence/student certificates require manual resolution';
    END IF;

    UPDATE certificate
    SET state = CASE
        WHEN LOWER(state) = 'revoked' THEN 'issued'
        ELSE LOWER(state)
    END;

    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'certificate'
          AND constraint_name = 'ck_certificate_issued_fields'
    ) THEN
        ALTER TABLE certificate DROP CHECK ck_certificate_issued_fields;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'certificate'
          AND constraint_name = 'ck_certificate_state'
    ) THEN
        ALTER TABLE certificate DROP CHECK ck_certificate_state;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'certificate'
          AND index_name = 'uq_certificate_course_active_student'
    ) THEN
        ALTER TABLE certificate DROP INDEX uq_certificate_course_active_student;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'certificate'
          AND column_name = 'active_student_user_id'
    ) THEN
        ALTER TABLE certificate DROP COLUMN active_student_user_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'certificate'
          AND column_name = 'revoked_at'
    ) THEN
        ALTER TABLE certificate DROP COLUMN revoked_at;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'certificate'
          AND index_name = 'uq_certificate_occurrence_student'
          AND non_unique = 0
    ) THEN
        ALTER TABLE certificate
            ADD UNIQUE KEY uq_certificate_occurrence_student (id_course_occurrence, id_user_student);
    END IF;

    ALTER TABLE certificate
        ADD CONSTRAINT ck_certificate_state
            CHECK (state IN ('draft', 'active', 'issued')),
        ADD CONSTRAINT ck_certificate_issued_fields
            CHECK (
                (state = 'issued'
                    AND validation_code IS NOT NULL
                    AND issued_at IS NOT NULL
                    AND final_grade IS NOT NULL)
                OR (state IN ('draft', 'active')
                    AND validation_code IS NULL
                    AND issued_at IS NULL
                    AND final_grade IS NULL)
            );
END$$
DELIMITER ;

CALL gape_migrate_v003();
DROP PROCEDURE IF EXISTS gape_migrate_v003;

DROP TRIGGER IF EXISTS bi_certificate_validate;
DROP TRIGGER IF EXISTS bu_certificate_validate;

DELIMITER $$
CREATE TRIGGER bi_certificate_validate
BEFORE INSERT ON certificate
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;

    SELECT id_course
    INTO v_occurrence_course
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Certificate Course occurrence must belong to the Certificate Course';
    END IF;
END$$

CREATE TRIGGER bu_certificate_validate
BEFORE UPDATE ON certificate
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;

    SELECT id_course
    INTO v_occurrence_course
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Certificate Course occurrence must belong to the Certificate Course';
    END IF;
END$$
DELIMITER ;
