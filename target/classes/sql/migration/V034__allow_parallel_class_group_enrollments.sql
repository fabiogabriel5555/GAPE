-- A student can hold active enrollments in more than one class group from the
-- same course occurrence. Keep the individual class-group, capacity and
-- course-occurrence safeguards while removing the cross-group overlap guard.
DELIMITER $$
DROP TRIGGER IF EXISTS bi_enroll_class_group_validate$$
CREATE TRIGGER bi_enroll_class_group_validate
BEFORE INSERT ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_class_start DATE;
    DECLARE v_class_end DATE;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_course_occurrence, cg.starts_at, cg.ends_at,
           cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_course_occurrence_id, v_class_start, v_class_end,
         v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    SET NEW.start_date = v_class_start;
    SET NEW.end_date = CASE
        WHEN NEW.state = 'withdrawn' THEN LEAST(
            v_class_end,
            GREATEST(v_class_start, COALESCE(NULLIF(NEW.end_date, '1000-01-01'), CURRENT_DATE))
        )
        ELSE v_class_end
    END;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    SELECT COUNT(*)
    INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND (ec.start_date IS NULL OR NEW.start_date IS NULL OR ec.start_date <= NEW.start_date)
      AND (NEW.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= NEW.end_date);

    SELECT COUNT(*)
    INTO v_active_enrollments
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
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_class_start DATE;
    DECLARE v_class_end DATE;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_course_occurrence, cg.starts_at, cg.ends_at,
           cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_course_occurrence_id, v_class_start, v_class_end,
         v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    SET NEW.start_date = v_class_start;
    SET NEW.end_date = CASE
        WHEN NEW.state = 'withdrawn' THEN LEAST(
            v_class_end,
            GREATEST(v_class_start, COALESCE(NULLIF(NEW.end_date, '1000-01-01'), CURRENT_DATE))
        )
        ELSE v_class_end
    END;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    SELECT COUNT(*)
    INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND (ec.start_date IS NULL OR NEW.start_date IS NULL OR ec.start_date <= NEW.start_date)
      AND (NEW.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= NEW.end_date);

    SELECT COUNT(*)
    INTO v_active_enrollments
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

    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$
DELIMITER ;
