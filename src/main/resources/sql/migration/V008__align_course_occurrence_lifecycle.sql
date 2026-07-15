ALTER TABLE course_occurrence
    DROP CHECK ck_course_occurrence_state,
    ADD CONSTRAINT ck_course_occurrence_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'inactive', 'cancelled'));

ALTER TABLE course_occurrence_period
    DROP CHECK ck_course_occurrence_period_state,
    ADD CONSTRAINT ck_course_occurrence_period_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'inactive', 'cancelled'));

DELIMITER $$
DROP TRIGGER IF EXISTS bi_course_occurrence_validate$$
CREATE TRIGGER bi_course_occurrence_validate
BEFORE INSERT ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_min_period_start DATE;
    DECLARE v_max_period_end DATE;
    DECLARE v_latest_occurrence_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

    SELECT
        MIN(STR_TO_DATE(CONCAT(
            YEAR(NEW.starts_at) + cpt.curricular_year - 1,
            '-', LPAD(cpt.starts_month, 2, '0'), '-', LPAD(cpt.starts_day, 2, '0')
        ), '%Y-%m-%d')),
        MAX(STR_TO_DATE(CONCAT(
            YEAR(NEW.starts_at) + cpt.curricular_year - 1,
            '-', LPAD(cpt.ends_month, 2, '0'), '-', LPAD(cpt.ends_day, 2, '0')
        ), '%Y-%m-%d'))
    INTO v_min_period_start, v_max_period_end
    FROM course_period_template cpt
    WHERE cpt.id_course = NEW.id_course;

    IF v_min_period_start IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence requires configured Course periods';
    END IF;

    IF NEW.starts_at > v_min_period_start OR NEW.ends_at < v_max_period_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence dates must cover all configured Course periods';
    END IF;

    SELECT MAX(ends_at)
    INTO v_latest_occurrence_end
    FROM course_occurrence
    WHERE id_course = NEW.id_course;

    IF v_latest_occurrence_end IS NOT NULL AND NEW.starts_at <= v_latest_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence must start after the previous occurrence ends';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_course_occurrence_validate$$
CREATE TRIGGER bu_course_occurrence_validate
BEFORE UPDATE ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_min_period_start DATE;
    DECLARE v_max_period_end DATE;
    DECLARE v_latest_occurrence_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

    SELECT
        MIN(STR_TO_DATE(CONCAT(
            YEAR(NEW.starts_at) + cpt.curricular_year - 1,
            '-', LPAD(cpt.starts_month, 2, '0'), '-', LPAD(cpt.starts_day, 2, '0')
        ), '%Y-%m-%d')),
        MAX(STR_TO_DATE(CONCAT(
            YEAR(NEW.starts_at) + cpt.curricular_year - 1,
            '-', LPAD(cpt.ends_month, 2, '0'), '-', LPAD(cpt.ends_day, 2, '0')
        ), '%Y-%m-%d'))
    INTO v_min_period_start, v_max_period_end
    FROM course_period_template cpt
    WHERE cpt.id_course = NEW.id_course;

    IF v_min_period_start IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence requires configured Course periods';
    END IF;

    IF NEW.starts_at > v_min_period_start OR NEW.ends_at < v_max_period_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence dates must cover all configured Course periods';
    END IF;

    SELECT MAX(ends_at)
    INTO v_latest_occurrence_end
    FROM course_occurrence
    WHERE id_course = NEW.id_course
      AND id_course_occurrence <> NEW.id_course_occurrence;

    IF v_latest_occurrence_end IS NOT NULL AND NEW.starts_at <= v_latest_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence must start after the previous occurrence ends';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence_period cop
        WHERE cop.id_course_occurrence = NEW.id_course_occurrence
          AND (NEW.starts_at > cop.starts_at OR NEW.ends_at < cop.ends_at)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence dates must cover all occurrence periods';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence_period cop
        JOIN course_period_template cpt
          ON cpt.id_course = NEW.id_course
         AND cpt.curricular_year = cop.curricular_year
         AND cpt.term = cop.term
        WHERE cop.id_course_occurrence = NEW.id_course_occurrence
          AND (
            cop.starts_at <> STR_TO_DATE(CONCAT(
                YEAR(NEW.starts_at) + cop.curricular_year - 1,
                '-', LPAD(cpt.starts_month, 2, '0'), '-', LPAD(cpt.starts_day, 2, '0')
            ), '%Y-%m-%d')
            OR cop.ends_at <> STR_TO_DATE(CONCAT(
                YEAR(NEW.starts_at) + cop.curricular_year - 1,
                '-', LPAD(cpt.ends_month, 2, '0'), '-', LPAD(cpt.ends_day, 2, '0')
            ), '%Y-%m-%d')
          )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing occurrence periods must follow Course period configuration';
    END IF;
END$$
DELIMITER ;

UPDATE course_occurrence
SET state = 'cancelled'
WHERE state = 'inactive';

UPDATE course_occurrence_period
SET state = 'cancelled'
WHERE state = 'inactive';

ALTER TABLE course_occurrence
    DROP CHECK ck_course_occurrence_state,
    ADD CONSTRAINT ck_course_occurrence_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'cancelled'));

ALTER TABLE course_occurrence_period
    DROP CHECK ck_course_occurrence_period_state,
    ADD CONSTRAINT ck_course_occurrence_period_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'cancelled'));
