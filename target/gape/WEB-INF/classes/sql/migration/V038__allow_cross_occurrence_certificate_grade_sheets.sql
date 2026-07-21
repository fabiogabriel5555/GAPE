-- A course certificate aggregates the student's subject results across the
-- subject occurrences in which those results were earned.  The certificate's
-- own course occurrence is still validated by bi/ bu_certificate_validate, but
-- a linked subject grade sheet may belong to any occurrence of that course.

DROP TRIGGER IF EXISTS bi_bgsc_validate;
DROP TRIGGER IF EXISTS bu_bgsc_validate;

DELIMITER $$
CREATE TRIGGER bi_bgsc_validate
BEFORE INSERT ON based_on_grade_sheet_certificate
FOR EACH ROW
BEGIN
    DECLARE v_course BIGINT UNSIGNED;
    DECLARE v_subject BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT c.id_course, gs.id_subject
    INTO v_course, v_subject
    FROM certificate c
    JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
    WHERE c.id_certificate = NEW.id_certificate;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = v_course
      AND id_subject = v_subject;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course must integrate the Subject of the referenced Grade_Sheet';
    END IF;
END$$

CREATE TRIGGER bu_bgsc_validate
BEFORE UPDATE ON based_on_grade_sheet_certificate
FOR EACH ROW
BEGIN
    DECLARE v_course BIGINT UNSIGNED;
    DECLARE v_subject BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT c.id_course, gs.id_subject
    INTO v_course, v_subject
    FROM certificate c
    JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
    WHERE c.id_certificate = NEW.id_certificate;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = v_course
      AND id_subject = v_subject;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course must integrate the Subject of the referenced Grade_Sheet';
    END IF;
END$$
DELIMITER ;
