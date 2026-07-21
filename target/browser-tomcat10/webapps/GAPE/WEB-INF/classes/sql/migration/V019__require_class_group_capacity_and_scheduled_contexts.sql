-- Class-group capacity is a real operating limit: both values are mandatory,
-- the minimum is positive and the maximum is strictly greater than it.
-- Normalize any legacy rows before strengthening the table constraint.
UPDATE class_group
SET min_students = 1
WHERE min_students IS NULL OR min_students <= 0;

UPDATE class_group
SET max_students = min_students + 1
WHERE max_students IS NULL OR max_students <= min_students;

ALTER TABLE class_group
    DROP CHECK ck_class_group_students_range,
    MODIFY min_students INT NOT NULL,
    MODIFY max_students INT NOT NULL,
    ADD CONSTRAINT ck_class_group_students_range
        CHECK (min_students > 0 AND max_students > min_students);

-- A future period creates a scheduled class group, so scheduled contexts are
-- valid. Active class groups still require an active occurrence and period.
DELIMITER $$
DROP TRIGGER IF EXISTS bi_class_group_validate$$
CREATE TRIGGER bi_class_group_validate
BEFORE INSERT ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_year INT;
    DECLARE v_association_term VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_period_occurrence BIGINT UNSIGNED;
    DECLARE v_period_year INT;
    DECLARE v_period_term VARCHAR(20);
    DECLARE v_period_start DATE;
    DECLARE v_period_end DATE;
    DECLARE v_period_state VARCHAR(20);

    SELECT c.state, s.state, isub.curricular_year, isub.term
    INTO v_course_state, v_subject_state, v_association_year, v_association_term
    FROM integrate_subject isub
    JOIN course c ON c.id_course = isub.id_course
    JOIN subject s ON s.id_subject = isub.id_subject
    WHERE isub.id_course = NEW.id_course
      AND isub.id_subject = NEW.id_subject;

    SELECT id_course, state
    INTO v_occurrence_course, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT id_course_occurrence, curricular_year, term, starts_at, ends_at, state
    INTO v_period_occurrence, v_period_year, v_period_term, v_period_start, v_period_end, v_period_state
    FROM course_occurrence_period
    WHERE id_course_occurrence_period = NEW.id_course_occurrence_period;

    IF v_association_year IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course must integrate the selected Subject';
    END IF;
    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course occurrence must belong to the selected Course';
    END IF;
    IF v_period_occurrence <> NEW.id_course_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must belong to the selected Course occurrence';
    END IF;
    IF v_association_year <> v_period_year OR v_association_term <> v_period_term THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must match the Subject curricular position in the Course';
    END IF;
    IF NEW.starts_at <> v_period_start OR NEW.ends_at <> v_period_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group dates must match the selected Course occurrence period';
    END IF;
    IF NEW.state = 'scheduled'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state NOT IN ('scheduled', 'active')
            OR v_period_state NOT IN ('scheduled', 'active')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduled Class_Group requires a scheduled or active occurrence context';
    END IF;
    IF NEW.state = 'active'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state <> 'active'
            OR v_period_state <> 'active') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group requires an active occurrence context';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_class_group_validate$$
CREATE TRIGGER bu_class_group_validate
BEFORE UPDATE ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_year INT;
    DECLARE v_association_term VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_period_occurrence BIGINT UNSIGNED;
    DECLARE v_period_year INT;
    DECLARE v_period_term VARCHAR(20);
    DECLARE v_period_start DATE;
    DECLARE v_period_end DATE;
    DECLARE v_period_state VARCHAR(20);
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT c.state, s.state, isub.curricular_year, isub.term
    INTO v_course_state, v_subject_state, v_association_year, v_association_term
    FROM integrate_subject isub
    JOIN course c ON c.id_course = isub.id_course
    JOIN subject s ON s.id_subject = isub.id_subject
    WHERE isub.id_course = NEW.id_course
      AND isub.id_subject = NEW.id_subject;

    SELECT id_course, state
    INTO v_occurrence_course, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT id_course_occurrence, curricular_year, term, starts_at, ends_at, state
    INTO v_period_occurrence, v_period_year, v_period_term, v_period_start, v_period_end, v_period_state
    FROM course_occurrence_period
    WHERE id_course_occurrence_period = NEW.id_course_occurrence_period;

    IF v_association_year IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course must integrate the selected Subject';
    END IF;
    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course occurrence must belong to the selected Course';
    END IF;
    IF v_period_occurrence <> NEW.id_course_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must belong to the selected Course occurrence';
    END IF;
    IF v_association_year <> v_period_year OR v_association_term <> v_period_term THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must match the Subject curricular position in the Course';
    END IF;
    IF NEW.starts_at <> v_period_start OR NEW.ends_at <> v_period_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group dates must match the selected Course occurrence period';
    END IF;
    IF NEW.state = 'scheduled'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state NOT IN ('scheduled', 'active')
            OR v_period_state NOT IN ('scheduled', 'active')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduled Class_Group requires a scheduled or active occurrence context';
    END IF;
    IF NEW.state = 'active'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state <> 'active'
            OR v_period_state <> 'active') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group requires an active occurrence context';
    END IF;

    SELECT COUNT(*)
    INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group
      AND state = 'active';
    IF v_active_enrollments > NEW.max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group max_students cannot be below active enrollments';
    END IF;
END$$
DELIMITER ;
