DROP TRIGGER IF EXISTS bi_receive_message_validate;
DROP PROCEDURE IF EXISTS gape_migrate_v006;

DELIMITER $$
CREATE PROCEDURE gape_migrate_v006()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'receive_message'
          AND constraint_name = 'ck_receive_message_state'
    ) THEN
        ALTER TABLE receive_message DROP CHECK ck_receive_message_state;
    END IF;

    UPDATE receive_message
    SET delivered_at = NULL,
        read_at = NULL,
        state = 'pending'
    WHERE state = 'failed';

    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'receive_message'
          AND constraint_name = 'ck_receive_message_delivery_mode'
    ) THEN
        ALTER TABLE receive_message DROP CHECK ck_receive_message_delivery_mode;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'receive_message'
          AND column_name = 'delivery_mode'
    ) THEN
        ALTER TABLE receive_message DROP COLUMN delivery_mode;
    END IF;

    ALTER TABLE receive_message
        ADD CONSTRAINT ck_receive_message_state
            CHECK (state IN ('pending', 'delivered', 'read'));
END$$
DELIMITER ;

CALL gape_migrate_v006();
DROP PROCEDURE IF EXISTS gape_migrate_v006;

DELIMITER $$
CREATE TRIGGER bi_receive_message_validate
BEFORE INSERT ON receive_message
FOR EACH ROW
BEGIN
    IF NEW.state = 'delivered' AND NEW.delivered_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Delivered Receive_Message requires delivered_at';
    END IF;

    IF NEW.state = 'read' AND (NEW.delivered_at IS NULL OR NEW.read_at IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Read Receive_Message requires delivered_at and read_at';
    END IF;
END$$
DELIMITER ;
