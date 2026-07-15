-- A class group is delivered in one concrete course occurrence only.  The
-- direct columns on class_group are the canonical context; the former
-- many-course projection is removed rather than retained as legacy data.
DELIMITER $$
DROP PROCEDURE IF EXISTS gape_assert_single_course_class_groups$$
CREATE PROCEDURE gape_assert_single_course_class_groups()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM class_group class_group_row
        LEFT JOIN course_occurrence occurrence_row
          ON occurrence_row.id_course_occurrence = class_group_row.id_course_occurrence
        LEFT JOIN course_occurrence_period period_row
          ON period_row.id_course_occurrence_period = class_group_row.id_course_occurrence_period
        LEFT JOIN integrate_subject association_row
          ON association_row.id_course = class_group_row.id_course
         AND association_row.id_subject = class_group_row.id_subject
        WHERE occurrence_row.id_course_occurrence IS NULL
           OR occurrence_row.id_course <> class_group_row.id_course
           OR period_row.id_course_occurrence <> class_group_row.id_course_occurrence
           OR period_row.curricular_year <> association_row.curricular_year
           OR period_row.term <> association_row.term
           OR class_group_row.starts_at <> period_row.starts_at
           OR class_group_row.ends_at <> period_row.ends_at
           OR association_row.id_course IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Every Class_Group must retain one valid direct Course occurrence context';
    END IF;
END$$
CALL gape_assert_single_course_class_groups()$$
DROP PROCEDURE gape_assert_single_course_class_groups$$
DELIMITER ;

DROP TABLE IF EXISTS class_group_course_occurrence;

DELIMITER $$
DROP TRIGGER IF EXISTS bi_class_group_validate$$
CREATE TRIGGER bi_class_group_validate
BEFORE INSERT ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_state VARCHAR(20);
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

    SELECT c.state, s.state, isub.state, isub.curricular_year, isub.term
    INTO v_course_state, v_subject_state, v_association_state, v_association_year, v_association_term
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
    IF v_association_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'New Class_Group requires an active Course_Subject association';
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
    DECLARE v_association_state VARCHAR(20);
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

    SELECT c.state, s.state, isub.state, isub.curricular_year, isub.term
    INTO v_course_state, v_subject_state, v_association_state, v_association_year, v_association_term
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
    IF v_association_state <> 'active'
       AND (NEW.id_course <> OLD.id_course
            OR NEW.id_subject <> OLD.id_subject
            OR NEW.id_course_occurrence <> OLD.id_course_occurrence
            OR NEW.id_course_occurrence_period <> OLD.id_course_occurrence_period) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Historical Course_Subject associations cannot receive or move Class_Groups';
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
