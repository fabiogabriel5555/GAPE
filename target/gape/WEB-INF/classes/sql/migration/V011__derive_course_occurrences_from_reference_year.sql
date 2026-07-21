SET @gape_has_reference_year = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'course_occurrence'
      AND column_name = 'reference_year'
);
SET @gape_reference_year_sql = IF(
    @gape_has_reference_year = 0,
    'ALTER TABLE course_occurrence ADD COLUMN reference_year INT NULL AFTER id_course',
    'SELECT 1'
);
PREPARE gape_reference_year_statement FROM @gape_reference_year_sql;
EXECUTE gape_reference_year_statement;
DEALLOCATE PREPARE gape_reference_year_statement;

UPDATE course_occurrence
SET reference_year = YEAR(starts_at)
WHERE reference_year IS NULL;

ALTER TABLE course_occurrence
    MODIFY COLUMN reference_year INT NOT NULL;

SET @gape_has_reference_year_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'course_occurrence'
      AND index_name = 'uq_course_occurrence_reference_year'
);
SET @gape_reference_year_index_sql = IF(
    @gape_has_reference_year_index = 0,
    'ALTER TABLE course_occurrence ADD UNIQUE KEY uq_course_occurrence_reference_year (id_course, reference_year)',
    'SELECT 1'
);
PREPARE gape_reference_year_index_statement FROM @gape_reference_year_index_sql;
EXECUTE gape_reference_year_index_statement;
DEALLOCATE PREPARE gape_reference_year_index_statement;

DELIMITER $$
DROP TRIGGER IF EXISTS bi_course_period_template_validate$$
CREATE TRIGGER bi_course_period_template_validate
BEFORE INSERT ON course_period_template
FOR EACH ROW
BEGIN
    DECLARE v_duration INT;
    DECLARE v_frequency VARCHAR(20);
    DECLARE v_start_date DATE;
    DECLARE v_end_date DATE;

    SELECT CAST(duration AS UNSIGNED), frequency
    INTO v_duration, v_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period curriculum level must be inside Course duration';
    END IF;

    IF (v_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period term must match Course frequency';
    END IF;

    SET v_start_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.starts_month, 2, '0'), '-', LPAD(NEW.starts_day, 2, '0')), '%Y-%m-%d');
    SET v_end_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.ends_month, 2, '0'), '-', LPAD(NEW.ends_day, 2, '0')), '%Y-%m-%d');
    IF v_start_date IS NULL OR MONTH(v_start_date) <> NEW.starts_month OR DAY(v_start_date) <> NEW.starts_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period start month/day is invalid';
    END IF;
    IF v_end_date IS NULL OR MONTH(v_end_date) <> NEW.ends_month OR DAY(v_end_date) <> NEW.ends_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period end month/day is invalid';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_course_period_template_validate$$
CREATE TRIGGER bu_course_period_template_validate
BEFORE UPDATE ON course_period_template
FOR EACH ROW
BEGIN
    DECLARE v_duration INT;
    DECLARE v_frequency VARCHAR(20);
    DECLARE v_start_date DATE;
    DECLARE v_end_date DATE;

    SELECT CAST(duration AS UNSIGNED), frequency
    INTO v_duration, v_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period curriculum level must be inside Course duration';
    END IF;

    IF (v_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period term must match Course frequency';
    END IF;

    SET v_start_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.starts_month, 2, '0'), '-', LPAD(NEW.starts_day, 2, '0')), '%Y-%m-%d');
    SET v_end_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.ends_month, 2, '0'), '-', LPAD(NEW.ends_day, 2, '0')), '%Y-%m-%d');
    IF v_start_date IS NULL OR MONTH(v_start_date) <> NEW.starts_month OR DAY(v_start_date) <> NEW.starts_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period start month/day is invalid';
    END IF;
    IF v_end_date IS NULL OR MONTH(v_end_date) <> NEW.ends_month OR DAY(v_end_date) <> NEW.ends_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period end month/day is invalid';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_course_occurrence_validate$$
CREATE TRIGGER bi_course_occurrence_validate
BEFORE INSERT ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_latest_occurrence_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

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
    DECLARE v_latest_occurrence_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

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
END$$

DROP TRIGGER IF EXISTS bi_course_occurrence_period_validate$$
CREATE TRIGGER bi_course_occurrence_period_validate
BEFORE INSERT ON course_occurrence_period
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_occurrence_state VARCHAR(20);

    SELECT id_course, starts_at, ends_at, state
    INTO v_occurrence_course, v_occurrence_start, v_occurrence_end, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF NOT EXISTS (
        SELECT 1
        FROM course_period_template
        WHERE id_course = v_occurrence_course
          AND curricular_year = NEW.curricular_year
          AND term = NEW.term
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must match a configured Course period';
    END IF;
    IF NEW.starts_at < v_occurrence_start OR NEW.ends_at > v_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must stay inside the Course occurrence date range';
    END IF;
    IF NEW.state IN ('active', 'completed') AND v_occurrence_state NOT IN ('active', 'completed') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active or completed Course occurrence period requires an active or completed Course occurrence';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_course_occurrence_period_validate$$
CREATE TRIGGER bu_course_occurrence_period_validate
BEFORE UPDATE ON course_occurrence_period
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_occurrence_state VARCHAR(20);

    SELECT id_course, starts_at, ends_at, state
    INTO v_occurrence_course, v_occurrence_start, v_occurrence_end, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF NOT EXISTS (
        SELECT 1
        FROM course_period_template
        WHERE id_course = v_occurrence_course
          AND curricular_year = NEW.curricular_year
          AND term = NEW.term
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must match a configured Course period';
    END IF;
    IF NEW.starts_at < v_occurrence_start OR NEW.ends_at > v_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must stay inside the Course occurrence date range';
    END IF;
    IF NEW.state IN ('active', 'completed') AND v_occurrence_state NOT IN ('active', 'completed') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active or completed Course occurrence period requires an active or completed Course occurrence';
    END IF;
END$$
DELIMITER ;
