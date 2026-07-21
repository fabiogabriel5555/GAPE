DROP PROCEDURE IF EXISTS gape_migrate_v005;

DELIMITER $$
CREATE PROCEDURE gape_migrate_v005()
BEGIN
    DECLARE v_invalid BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_invalid
    FROM direct_message_channel dmc
    LEFT JOIN channel c ON c.id_channel = dmc.id_channel
    WHERE dmc.id_user_low >= dmc.id_user_high
       OR c.id_channel IS NULL
       OR c.state <> 'active'
       OR c.type <> 'message'
       OR c.visibility <> 'participants'
       OR (SELECT COUNT(*) FROM participate_channel pc
           WHERE pc.id_channel = dmc.id_channel AND pc.state = 'active') <> 2
       OR NOT EXISTS (SELECT 1 FROM participate_channel pc
                      WHERE pc.id_channel = dmc.id_channel
                        AND pc.id_user = dmc.id_user_low
                        AND pc.state = 'active')
       OR NOT EXISTS (SELECT 1 FROM participate_channel pc
                      WHERE pc.id_channel = dmc.id_channel
                        AND pc.id_user = dmc.id_user_high
                        AND pc.state = 'active')
       OR EXISTS (SELECT 1 FROM associate_channel_class_group acg
                  WHERE acg.id_channel = dmc.id_channel)
       OR EXISTS (SELECT 1 FROM associate_channel_content_block acb
                  WHERE acb.id_channel = dmc.id_channel)
       OR EXISTS (SELECT 1 FROM associate_channel_assessment aa
                  WHERE aa.id_channel = dmc.id_channel);

    IF v_invalid > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message migration stopped: registered channels violate participant or context invariants';
    END IF;
END$$
DELIMITER ;

CALL gape_migrate_v005();
DROP PROCEDURE IF EXISTS gape_migrate_v005;

DROP TRIGGER IF EXISTS bu_registered_direct_channel_validate;
DROP TRIGGER IF EXISTS bd_registered_direct_channel_validate;
DROP TRIGGER IF EXISTS bi_direct_participation_validate;
DROP TRIGGER IF EXISTS bu_direct_participation_validate;
DROP TRIGGER IF EXISTS bd_direct_participation_validate;
DROP TRIGGER IF EXISTS bi_direct_channel_class_group_validate;
DROP TRIGGER IF EXISTS bu_direct_channel_class_group_validate;
DROP TRIGGER IF EXISTS bi_direct_channel_content_block_validate;
DROP TRIGGER IF EXISTS bu_direct_channel_content_block_validate;
DROP TRIGGER IF EXISTS bi_direct_channel_assessment_validate;
DROP TRIGGER IF EXISTS bu_direct_channel_assessment_validate;
DROP TRIGGER IF EXISTS bi_direct_message_channel_validate;
DROP TRIGGER IF EXISTS bu_direct_message_channel_validate;

DELIMITER $$
CREATE TRIGGER bu_registered_direct_channel_validate
BEFORE UPDATE ON channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = OLD.id_channel
    ) AND (
        NEW.id_channel <> OLD.id_channel
        OR NEW.state <> 'active'
        OR NEW.type <> 'message'
        OR NEW.visibility <> 'participants'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registered direct-message channels must remain active message channels for participants';
    END IF;
END$$

CREATE TRIGGER bd_registered_direct_channel_validate
BEFORE DELETE ON channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = OLD.id_channel
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unregister a direct-message channel before deleting it';
    END IF;
END$$

CREATE TRIGGER bi_direct_participation_validate
BEFORE INSERT ON participate_channel
FOR EACH ROW
BEGIN
    IF NEW.state = 'active' AND EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = NEW.id_channel
          AND NEW.id_user NOT IN (dmc.id_user_low, dmc.id_user_high)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registered direct-message channels cannot have a third active participant';
    END IF;
END$$

CREATE TRIGGER bu_direct_participation_validate
BEFORE UPDATE ON participate_channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = OLD.id_channel
          AND OLD.id_user IN (dmc.id_user_low, dmc.id_user_high)
    ) AND (
        NEW.id_channel <> OLD.id_channel
        OR NEW.id_user <> OLD.id_user
        OR NEW.state <> 'active'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required direct-message participants must remain active';
    END IF;

    IF NEW.state = 'active' AND EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = NEW.id_channel
          AND NEW.id_user NOT IN (dmc.id_user_low, dmc.id_user_high)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registered direct-message channels cannot have a third active participant';
    END IF;
END$$

CREATE TRIGGER bd_direct_participation_validate
BEFORE DELETE ON participate_channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = OLD.id_channel
          AND OLD.id_user IN (dmc.id_user_low, dmc.id_user_high)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required direct-message participants cannot be removed';
    END IF;
END$$

CREATE TRIGGER bi_direct_channel_class_group_validate
BEFORE INSERT ON associate_channel_class_group
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a class group';
    END IF;
END$$

CREATE TRIGGER bu_direct_channel_class_group_validate
BEFORE UPDATE ON associate_channel_class_group
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a class group';
    END IF;
END$$

CREATE TRIGGER bi_direct_channel_content_block_validate
BEFORE INSERT ON associate_channel_content_block
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a content block';
    END IF;
END$$

CREATE TRIGGER bu_direct_channel_content_block_validate
BEFORE UPDATE ON associate_channel_content_block
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a content block';
    END IF;
END$$

CREATE TRIGGER bi_direct_channel_assessment_validate
BEFORE INSERT ON associate_channel_assessment
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with an assessment';
    END IF;
END$$

CREATE TRIGGER bu_direct_channel_assessment_validate
BEFORE UPDATE ON associate_channel_assessment
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with an assessment';
    END IF;
END$$

CREATE TRIGGER bi_direct_message_channel_validate
BEFORE INSERT ON direct_message_channel
FOR EACH ROW
BEGIN
    DECLARE v_valid_channel INT DEFAULT 0;

    IF NEW.id_user_low >= NEW.id_user_high THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Direct message participants must be stored in ascending order';
    END IF;

    SELECT COUNT(*)
    INTO v_valid_channel
    FROM channel c
    WHERE c.id_channel = NEW.id_channel
      AND c.state = 'active'
      AND c.type = 'message'
      AND c.visibility = 'participants'
      AND (SELECT COUNT(*) FROM participate_channel pc
           WHERE pc.id_channel = c.id_channel AND pc.state = 'active') = 2
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_low AND pc.state = 'active')
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_high AND pc.state = 'active')
      AND NOT EXISTS (SELECT 1 FROM associate_channel_class_group acg WHERE acg.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_content_block acb WHERE acb.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_assessment aa WHERE aa.id_channel = c.id_channel);

    IF v_valid_channel = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message registration requires one active context-free channel with exactly both participants';
    END IF;
END$$

CREATE TRIGGER bu_direct_message_channel_validate
BEFORE UPDATE ON direct_message_channel
FOR EACH ROW
BEGIN
    DECLARE v_valid_channel INT DEFAULT 0;

    IF NEW.id_user_low >= NEW.id_user_high THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Direct message participants must be stored in ascending order';
    END IF;

    SELECT COUNT(*)
    INTO v_valid_channel
    FROM channel c
    WHERE c.id_channel = NEW.id_channel
      AND c.state = 'active'
      AND c.type = 'message'
      AND c.visibility = 'participants'
      AND (SELECT COUNT(*) FROM participate_channel pc
           WHERE pc.id_channel = c.id_channel AND pc.state = 'active') = 2
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_low AND pc.state = 'active')
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_high AND pc.state = 'active')
      AND NOT EXISTS (SELECT 1 FROM associate_channel_class_group acg WHERE acg.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_content_block acb WHERE acb.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_assessment aa WHERE aa.id_channel = c.id_channel);

    IF v_valid_channel = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message registration requires one active context-free channel with exactly both participants';
    END IF;
END$$
DELIMITER ;
