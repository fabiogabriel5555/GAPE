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

    SELECT state
    INTO v_course_state
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_course, state, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_state, v_occurrence_start, v_occurrence_end
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment occurrence must belong to the selected Course';
    END IF;

    SET NEW.start_date = v_occurrence_start;
    SET NEW.end_date = v_occurrence_end;

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

    SELECT state
    INTO v_course_state
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_course, state, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_state, v_occurrence_start, v_occurrence_end
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment occurrence must belong to the selected Course';
    END IF;

    SET NEW.start_date = v_occurrence_start;
    SET NEW.end_date = v_occurrence_end;

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
DELIMITER ;

INSERT INTO grade_sheet (
    id_subject,
    id_course_occurrence,
    title,
    type,
    max_grade,
    passing_grade,
    released_at,
    state
)
SELECT
    subject.id_subject,
    occurrence.id_course_occurrence,
    CONCAT('Pauta - ', subject.name),
    'final',
    COALESCE(NULLIF(subject.final_grade_max, 0), 20.00),
    CASE
        WHEN COALESCE(NULLIF(subject.final_grade_max, 0), 20.00) = 20.00 THEN 9.50
        ELSE ROUND(COALESCE(NULLIF(subject.final_grade_max, 0), 20.00) / 2, 2)
    END,
    NULL,
    'draft'
FROM course_occurrence occurrence
JOIN integrate_subject course_subject ON course_subject.id_course = occurrence.id_course
JOIN subject ON subject.id_subject = course_subject.id_subject
WHERE subject.state = 'active'
  AND NOT EXISTS (
      SELECT 1
      FROM grade_sheet existing_sheet
      WHERE existing_sheet.id_subject = subject.id_subject
        AND existing_sheet.id_course_occurrence = occurrence.id_course_occurrence
        AND NOT EXISTS (
            SELECT 1
            FROM associate_grade_sheet_class_group sheet_class_group
            WHERE sheet_class_group.id_grade_sheet = existing_sheet.id_grade_sheet
        )
  );
