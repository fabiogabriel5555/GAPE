-- A submitted attempt remains correctable after its assessment is completed.
-- The assessment lifecycle changes its enrollment from active to completed;
-- that historical enrollment must still satisfy the attempt integrity trigger.
DELIMITER $$

DROP TRIGGER IF EXISTS bi_attempt_validate$$
CREATE TRIGGER bi_attempt_validate
BEFORE INSERT ON attempt
FOR EACH ROW
BEGIN
    DECLARE v_attempts_limit INT;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_class_group BIGINT UNSIGNED;
    DECLARE v_assessment_state VARCHAR(20);
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_assessment_enrollment_exists INT DEFAULT 0;

    SELECT a.attempts_limit, a.id_subject, cb.id_class_group, a.state
    INTO v_attempts_limit, v_assessment_subject, v_class_group, v_assessment_state
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_attempts_limit IS NOT NULL AND NEW.attempt_number > v_attempts_limit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt number exceeds Assessment.attempts_limit';
    END IF;

    IF NEW.state IN ('submitted', 'corrected') AND NEW.submitted_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted or corrected Attempt requires submitted_at';
    END IF;

    IF NEW.state <> 'corrected' AND NEW.score IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only corrected Attempt can store score';
    END IF;

    IF v_class_group IS NOT NULL THEN
        SELECT COUNT(*) INTO v_exists
        FROM enroll_class_group
        WHERE id_student_user = NEW.id_student_user
          AND id_class_group = v_class_group
          AND (state = 'active' OR (v_assessment_state = 'completed' AND state = 'completed'));
    ELSEIF v_assessment_subject IS NOT NULL THEN
        SELECT COUNT(*) INTO v_exists
        FROM assessment_class_group acg
        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
        JOIN enroll_class_group ecg
          ON ecg.id_class_group = cg.id_class_group
         AND ecg.id_student_user = NEW.id_student_user
         AND (ecg.state = 'active' OR (v_assessment_state = 'completed' AND ecg.state = 'completed'))
        WHERE acg.id_assessment = NEW.id_assessment
          AND cg.id_subject = v_assessment_subject;
    END IF;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active enrollment in the Assessment context';
    END IF;

    SELECT COUNT(*) INTO v_assessment_enrollment_exists
    FROM enroll_assessment
    WHERE id_student_user = NEW.id_student_user
      AND id_assessment = NEW.id_assessment
      AND (state = 'active' OR (v_assessment_state = 'completed' AND state = 'completed'));

    IF v_assessment_enrollment_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active Assessment enrollment';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_attempt_validate$$
CREATE TRIGGER bu_attempt_validate
BEFORE UPDATE ON attempt
FOR EACH ROW
BEGIN
    DECLARE v_attempts_limit INT;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_class_group BIGINT UNSIGNED;
    DECLARE v_assessment_state VARCHAR(20);
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_assessment_enrollment_exists INT DEFAULT 0;

    SELECT a.attempts_limit, a.id_subject, cb.id_class_group, a.state
    INTO v_attempts_limit, v_assessment_subject, v_class_group, v_assessment_state
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_attempts_limit IS NOT NULL AND NEW.attempt_number > v_attempts_limit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt number exceeds Assessment.attempts_limit';
    END IF;

    IF NEW.state IN ('submitted', 'corrected') AND NEW.submitted_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted or corrected Attempt requires submitted_at';
    END IF;

    IF NEW.state <> 'corrected' AND NEW.score IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only corrected Attempt can store score';
    END IF;

    IF v_class_group IS NOT NULL THEN
        SELECT COUNT(*) INTO v_exists
        FROM enroll_class_group
        WHERE id_student_user = NEW.id_student_user
          AND id_class_group = v_class_group
          AND (state = 'active' OR (v_assessment_state = 'completed' AND state = 'completed'));
    ELSEIF v_assessment_subject IS NOT NULL THEN
        SELECT COUNT(*) INTO v_exists
        FROM assessment_class_group acg
        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
        JOIN enroll_class_group ecg
          ON ecg.id_class_group = cg.id_class_group
         AND ecg.id_student_user = NEW.id_student_user
         AND (ecg.state = 'active' OR (v_assessment_state = 'completed' AND ecg.state = 'completed'))
        WHERE acg.id_assessment = NEW.id_assessment
          AND cg.id_subject = v_assessment_subject;
    END IF;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active enrollment in the Assessment context';
    END IF;

    SELECT COUNT(*) INTO v_assessment_enrollment_exists
    FROM enroll_assessment
    WHERE id_student_user = NEW.id_student_user
      AND id_assessment = NEW.id_assessment
      AND (state = 'active' OR (v_assessment_state = 'completed' AND state = 'completed'));

    IF v_assessment_enrollment_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active Assessment enrollment';
    END IF;
END$$

DELIMITER ;
