DROP PROCEDURE IF EXISTS gape_migrate_v007;

DELIMITER $$
CREATE PROCEDURE gape_migrate_v007()
BEGIN
    DECLARE v_overlap_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_overlap_count
    FROM course_occurrence earlier
    JOIN course_occurrence later
      ON later.id_course = earlier.id_course
     AND later.id_course_occurrence > earlier.id_course_occurrence
     AND later.starts_at <= earlier.ends_at;

    IF v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Course-occurrence migration stopped: occurrence order or overlap requires manual resolution';
    END IF;
END$$
DELIMITER ;

CALL gape_migrate_v007();
DROP PROCEDURE IF EXISTS gape_migrate_v007;

ALTER TABLE course_occurrence
    MODIFY COLUMN label VARCHAR(220) NOT NULL;

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
        WHEN CURRENT_DATE < NEW.starts_at THEN 'draft'
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
        WHEN CURRENT_DATE < NEW.starts_at THEN 'draft'
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

DROP TRIGGER IF EXISTS bi_enroll_course_validate$$
CREATE TRIGGER bi_enroll_course_validate
BEFORE INSERT ON enroll_course
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_student_count INT DEFAULT 0;

    SELECT state INTO v_course_state
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_course, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_start, v_occurrence_end
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT COUNT(*) INTO v_student_count
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
        OR CURRENT_DATE < v_occurrence_start
        OR CURRENT_DATE > v_occurrence_end
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Course occurrence';
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
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_student_count INT DEFAULT 0;

    SELECT state INTO v_course_state
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_course, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_start, v_occurrence_end
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT COUNT(*) INTO v_student_count
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
        OR CURRENT_DATE < v_occurrence_start
        OR CURRENT_DATE > v_occurrence_end
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Course occurrence';
    END IF;

    IF NEW.state = 'active' AND v_student_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Student';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_enroll_class_group_validate$$
CREATE TRIGGER bi_enroll_class_group_validate
BEFORE INSERT ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_subject_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_subject, cg.id_course_occurrence, cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_subject_id, v_course_occurrence_id, v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    SELECT COUNT(*) INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    SELECT COUNT(*) INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND (ec.start_date IS NULL OR NEW.start_date IS NULL OR ec.start_date <= NEW.start_date)
      AND (NEW.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= NEW.end_date);

    SELECT COUNT(*) INTO v_overlap_count
    FROM enroll_class_group ecg
    JOIN class_group existing_cg ON existing_cg.id_class_group = ecg.id_class_group
    WHERE ecg.id_student_user = NEW.id_student_user
      AND existing_cg.id_course = v_course_id
      AND existing_cg.id_subject = v_subject_id
      AND existing_cg.id_course_occurrence = v_course_occurrence_id
      AND ecg.state = 'active'
      AND (ecg.start_date IS NULL OR NEW.end_date IS NULL OR ecg.start_date <= NEW.end_date)
      AND (ecg.end_date IS NULL OR NEW.start_date IS NULL OR ecg.end_date >= NEW.start_date);

    SELECT COUNT(*) INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group
      AND state = 'active';

    IF NEW.state = 'active'
       AND (v_student_count = 0
            OR v_class_state <> 'active'
            OR v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_association_course IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group enrollment requires active Student, Class_Group, Course, Subject and course-subject association';
    END IF;
    IF NEW.state = 'active' AND v_course_enrollment_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment requires active Course occurrence enrollment for the full period';
    END IF;
    IF NEW.state = 'active' AND v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Student already has an overlapping active enrollment in this Course/Subject class group context';
    END IF;
    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_enroll_class_group_validate$$
CREATE TRIGGER bu_enroll_class_group_validate
BEFORE UPDATE ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_subject_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_subject, cg.id_course_occurrence, cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_subject_id, v_course_occurrence_id, v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    SELECT COUNT(*) INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    SELECT COUNT(*) INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND (ec.start_date IS NULL OR NEW.start_date IS NULL OR ec.start_date <= NEW.start_date)
      AND (NEW.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= NEW.end_date);

    SELECT COUNT(*) INTO v_overlap_count
    FROM enroll_class_group ecg
    JOIN class_group existing_cg ON existing_cg.id_class_group = ecg.id_class_group
    WHERE ecg.id_student_user = NEW.id_student_user
      AND existing_cg.id_course = v_course_id
      AND existing_cg.id_subject = v_subject_id
      AND existing_cg.id_course_occurrence = v_course_occurrence_id
      AND ecg.state = 'active'
      AND NOT (ecg.id_student_user = OLD.id_student_user AND ecg.id_class_group = OLD.id_class_group)
      AND (ecg.start_date IS NULL OR NEW.end_date IS NULL OR ecg.start_date <= NEW.end_date)
      AND (ecg.end_date IS NULL OR NEW.start_date IS NULL OR ecg.end_date >= NEW.start_date);

    SELECT COUNT(*) INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group
      AND state = 'active'
      AND NOT (id_student_user = OLD.id_student_user AND id_class_group = OLD.id_class_group);

    IF NEW.state = 'active'
       AND (v_student_count = 0
            OR v_class_state <> 'active'
            OR v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_association_course IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group enrollment requires active Student, Class_Group, Course, Subject and course-subject association';
    END IF;
    IF NEW.state = 'active' AND v_course_enrollment_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment requires active Course occurrence enrollment for the full period';
    END IF;
    IF NEW.state = 'active' AND v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Student already has an overlapping active enrollment in this Course/Subject class group context';
    END IF;
    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$
DELIMITER ;
