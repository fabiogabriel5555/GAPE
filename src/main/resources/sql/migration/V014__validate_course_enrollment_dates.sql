-- Keep the submitted course-enrollment dates inside the selected occurrence.
-- A missing start date is completed from the occurrence; a missing end date
-- remains open-ended. Invalid supplied boundaries must be rejected instead of
-- being silently rewritten.
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

    IF NEW.start_date IS NOT NULL
       AND (NEW.start_date < v_occurrence_start OR NEW.start_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment start date must stay inside the Course occurrence';
    END IF;

    IF NEW.end_date IS NOT NULL
       AND (NEW.end_date < v_occurrence_start OR NEW.end_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment end date must stay inside the Course occurrence';
    END IF;

    SET NEW.start_date = COALESCE(NEW.start_date, v_occurrence_start);

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

    IF NEW.start_date IS NOT NULL
       AND (NEW.start_date < v_occurrence_start OR NEW.start_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment start date must stay inside the Course occurrence';
    END IF;

    IF NEW.end_date IS NOT NULL
       AND (NEW.end_date < v_occurrence_start OR NEW.end_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment end date must stay inside the Course occurrence';
    END IF;

    SET NEW.start_date = COALESCE(NEW.start_date, v_occurrence_start);

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
