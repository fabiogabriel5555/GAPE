-- Subject deactivation preserves its existing curricular and coordinator
-- records.  Only creation or reactivation of an active association requires
-- an active subject.
DELIMITER $$
DROP TRIGGER IF EXISTS bu_subject_validate$$
CREATE TRIGGER bu_subject_validate
BEFORE UPDATE ON subject
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

DROP TRIGGER IF EXISTS bi_coordinate_subject_validate$$
CREATE TRIGGER bi_coordinate_subject_validate
BEFORE INSERT ON coordinate_subject
FOR EACH ROW
BEGIN
    DECLARE v_coordinator_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);

    IF NEW.state = 'active' THEN
        SELECT state
        INTO v_coordinator_state
        FROM user_account
        WHERE id_user = NEW.id_coordinator_user;

        SELECT state
        INTO v_subject_state
        FROM subject
        WHERE id_subject = NEW.id_subject;

        IF v_coordinator_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Coordinator';
        END IF;
        IF v_subject_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Subject';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_coordinate_subject_validate$$
CREATE TRIGGER bu_coordinate_subject_validate
BEFORE UPDATE ON coordinate_subject
FOR EACH ROW
BEGIN
    DECLARE v_coordinator_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);

    IF NEW.state = 'active' AND OLD.state <> 'active' THEN
        SELECT state
        INTO v_coordinator_state
        FROM user_account
        WHERE id_user = NEW.id_coordinator_user;

        SELECT state
        INTO v_subject_state
        FROM subject
        WHERE id_subject = NEW.id_subject;

        IF v_coordinator_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Coordinator';
        END IF;
        IF v_subject_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Subject';
        END IF;
    END IF;
END$$
DELIMITER ;
