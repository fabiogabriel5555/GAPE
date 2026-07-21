-- A course occurrence is a concrete rendering of the course calendar. Its
-- reference year and label must therefore be derived from its dates, and each
-- occurrence period must reproduce the configured template exactly.
--
-- Normalize the descriptive fields before activating the stricter guards so
-- existing data is never retained under an inconsistent calendar label.
DROP TRIGGER IF EXISTS bu_course_occurrence_validate;

UPDATE course_occurrence
SET reference_year = YEAR(starts_at),
    label = CONCAT(
        YEAR(starts_at),
        IF(YEAR(ends_at) > YEAR(starts_at), CONCAT('-', YEAR(ends_at)), '')
    )
WHERE reference_year <> YEAR(starts_at)
   OR label <> CONCAT(
        YEAR(starts_at),
        IF(YEAR(ends_at) > YEAR(starts_at), CONCAT('-', YEAR(ends_at)), '')
   );

DELIMITER $$
DROP TRIGGER IF EXISTS bi_course_occurrence_validate$$
CREATE TRIGGER bi_course_occurrence_validate
BEFORE INSERT ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

    IF NEW.reference_year <> YEAR(NEW.starts_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence reference year must match its start year';
    END IF;

    IF NEW.label <> CONCAT(
        YEAR(NEW.starts_at),
        IF(YEAR(NEW.ends_at) > YEAR(NEW.starts_at), CONCAT('-', YEAR(NEW.ends_at)), '')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence label must match its concrete date range';
    END IF;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = NEW.id_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SELECT MIN(DATE_ADD(
               STR_TO_DATE(CONCAT(NEW.reference_year, '-', LPAD(starts_month, 2, '0'), '-', LPAD(starts_day, 2, '0')),
                           '%Y-%m-%d'),
               INTERVAL IF(
                   starts_month < v_anchor_start_month
                   OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                   1,
                   0
               ) YEAR
           )),
           MAX(DATE_ADD(
               STR_TO_DATE(CONCAT(
                   NEW.reference_year + IF(
                       starts_month < v_anchor_start_month
                       OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                       1,
                       0
                   ),
                   '-', LPAD(ends_month, 2, '0'), '-', LPAD(ends_day, 2, '0')
               ), '%Y-%m-%d'),
               INTERVAL IF(
                   ends_month < starts_month
                   OR (ends_month = starts_month AND ends_day < starts_day),
                   1,
                   0
               ) YEAR
           ))
    INTO v_expected_start, v_expected_end
    FROM course_period_template
    WHERE id_course = NEW.id_course;

    IF v_expected_start IS NULL
       OR NEW.starts_at <> v_expected_start
       OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence dates must match the configured Course calendar';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence existing_occurrence
        WHERE existing_occurrence.id_course = NEW.id_course
          AND NOT (
              NEW.ends_at < existing_occurrence.starts_at
              OR NEW.starts_at > existing_occurrence.ends_at
          )
    ) THEN
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

DROP TRIGGER IF EXISTS bu_course_occurrence_validate$$
CREATE TRIGGER bu_course_occurrence_validate
BEFORE UPDATE ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

    IF NEW.reference_year <> YEAR(NEW.starts_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence reference year must match its start year';
    END IF;

    IF NEW.label <> CONCAT(
        YEAR(NEW.starts_at),
        IF(YEAR(NEW.ends_at) > YEAR(NEW.starts_at), CONCAT('-', YEAR(NEW.ends_at)), '')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence label must match its concrete date range';
    END IF;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = NEW.id_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SELECT MIN(DATE_ADD(
               STR_TO_DATE(CONCAT(NEW.reference_year, '-', LPAD(starts_month, 2, '0'), '-', LPAD(starts_day, 2, '0')),
                           '%Y-%m-%d'),
               INTERVAL IF(
                   starts_month < v_anchor_start_month
                   OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                   1,
                   0
               ) YEAR
           )),
           MAX(DATE_ADD(
               STR_TO_DATE(CONCAT(
                   NEW.reference_year + IF(
                       starts_month < v_anchor_start_month
                       OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                       1,
                       0
                   ),
                   '-', LPAD(ends_month, 2, '0'), '-', LPAD(ends_day, 2, '0')
               ), '%Y-%m-%d'),
               INTERVAL IF(
                   ends_month < starts_month
                   OR (ends_month = starts_month AND ends_day < starts_day),
                   1,
                   0
               ) YEAR
           ))
    INTO v_expected_start, v_expected_end
    FROM course_period_template
    WHERE id_course = NEW.id_course;

    IF v_expected_start IS NULL
       OR NEW.starts_at <> v_expected_start
       OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence dates must match the configured Course calendar';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence existing_occurrence
        WHERE existing_occurrence.id_course = NEW.id_course
          AND existing_occurrence.id_course_occurrence <> NEW.id_course_occurrence
          AND NOT (
              NEW.ends_at < existing_occurrence.starts_at
              OR NEW.starts_at > existing_occurrence.ends_at
          )
    ) THEN
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
    DECLARE v_template_start_month TINYINT UNSIGNED;
    DECLARE v_template_start_day TINYINT UNSIGNED;
    DECLARE v_template_end_month TINYINT UNSIGNED;
    DECLARE v_template_end_day TINYINT UNSIGNED;
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;

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

    SELECT starts_month, starts_day, ends_month, ends_day
    INTO v_template_start_month, v_template_start_day, v_template_end_month, v_template_end_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
      AND curricular_year = NEW.curricular_year
      AND term = NEW.term;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SET v_expected_start = STR_TO_DATE(
        CONCAT(YEAR(v_occurrence_start), '-', LPAD(v_template_start_month, 2, '0'), '-', LPAD(v_template_start_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_template_start_month < v_anchor_start_month
       OR (v_template_start_month = v_anchor_start_month AND v_template_start_day < v_anchor_start_day) THEN
        SET v_expected_start = DATE_ADD(v_expected_start, INTERVAL 1 YEAR);
    END IF;
    SET v_expected_end = STR_TO_DATE(
        CONCAT(YEAR(v_expected_start), '-', LPAD(v_template_end_month, 2, '0'), '-', LPAD(v_template_end_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_expected_end < v_expected_start THEN
        SET v_expected_end = DATE_ADD(v_expected_end, INTERVAL 1 YEAR);
    END IF;

    IF NEW.starts_at <> v_expected_start OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence period dates must match the configured Course period';
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
    DECLARE v_template_start_month TINYINT UNSIGNED;
    DECLARE v_template_start_day TINYINT UNSIGNED;
    DECLARE v_template_end_month TINYINT UNSIGNED;
    DECLARE v_template_end_day TINYINT UNSIGNED;
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;

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

    SELECT starts_month, starts_day, ends_month, ends_day
    INTO v_template_start_month, v_template_start_day, v_template_end_month, v_template_end_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
      AND curricular_year = NEW.curricular_year
      AND term = NEW.term;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SET v_expected_start = STR_TO_DATE(
        CONCAT(YEAR(v_occurrence_start), '-', LPAD(v_template_start_month, 2, '0'), '-', LPAD(v_template_start_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_template_start_month < v_anchor_start_month
       OR (v_template_start_month = v_anchor_start_month AND v_template_start_day < v_anchor_start_day) THEN
        SET v_expected_start = DATE_ADD(v_expected_start, INTERVAL 1 YEAR);
    END IF;
    SET v_expected_end = STR_TO_DATE(
        CONCAT(YEAR(v_expected_start), '-', LPAD(v_template_end_month, 2, '0'), '-', LPAD(v_template_end_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_expected_end < v_expected_start THEN
        SET v_expected_end = DATE_ADD(v_expected_end, INTERVAL 1 YEAR);
    END IF;

    IF NEW.starts_at <> v_expected_start OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence period dates must match the configured Course period';
    END IF;

    IF NEW.starts_at < v_occurrence_start OR NEW.ends_at > v_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must stay inside the Course occurrence date range';
    END IF;

    IF NEW.state IN ('active', 'completed') AND v_occurrence_state NOT IN ('active', 'completed') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active or completed Course occurrence period requires an active or completed Course occurrence';
    END IF;
END$$
DELIMITER ;
