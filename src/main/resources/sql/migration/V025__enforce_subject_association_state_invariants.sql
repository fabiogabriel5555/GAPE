-- A subject may exist without a course.  Its state is otherwise independent
-- from its course associations, except that an active association requires an
-- active subject and an active association must be closed before inactivation.
-- Normalize rows created before this rule without discarding their history.
UPDATE integrate_subject association_row
JOIN subject subject_row
  ON subject_row.id_subject = association_row.id_subject
SET association_row.state = 'historical',
    association_row.ended_at = COALESCE(association_row.ended_at, CURRENT_DATE)
WHERE subject_row.state = 'inactive'
  AND association_row.state = 'active';

DELIMITER $$
DROP TRIGGER IF EXISTS bi_subject_validate$$
CREATE TRIGGER bi_subject_validate
BEFORE INSERT ON subject
FOR EACH ROW
BEGIN
    DECLARE v_organization_state VARCHAR(20);

    SELECT state
    INTO v_organization_state
    FROM organization
    WHERE id_organization = NEW.id_organization;

    IF NEW.state = 'active' AND v_organization_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject cannot be active in inactive Organization';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_subject_validate$$
CREATE TRIGGER bu_subject_validate
BEFORE UPDATE ON subject
FOR EACH ROW
BEGIN
    DECLARE v_organization_state VARCHAR(20);
    DECLARE v_active_course_associations INT DEFAULT 0;

    SELECT state
    INTO v_organization_state
    FROM organization
    WHERE id_organization = NEW.id_organization;

    SELECT COUNT(*)
    INTO v_active_course_associations
    FROM integrate_subject
    WHERE id_subject = NEW.id_subject
      AND state = 'active';

    IF NEW.state = 'active' AND v_organization_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject cannot be active in inactive Organization';
    END IF;
    IF NEW.state = 'inactive' AND v_active_course_associations > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Subject with active Course_Subject associations cannot be inactive';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_integrate_subject_validate$$
CREATE TRIGGER bi_integrate_subject_validate
BEFORE INSERT ON integrate_subject
FOR EACH ROW
BEGIN
    DECLARE v_course_org BIGINT UNSIGNED;
    DECLARE v_subject_org BIGINT UNSIGNED;
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT id_organization, state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_org, v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_organization, state
    INTO v_subject_org, v_subject_state
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_org <> v_subject_org THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course and Subject must belong to the same Organization';
    END IF;

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
    DECLARE v_course_org BIGINT UNSIGNED;
    DECLARE v_subject_org BIGINT UNSIGNED;
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT id_organization, state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_org, v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_organization, state
    INTO v_subject_org, v_subject_state
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_org <> v_subject_org THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course and Subject must belong to the same Organization';
    END IF;

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
