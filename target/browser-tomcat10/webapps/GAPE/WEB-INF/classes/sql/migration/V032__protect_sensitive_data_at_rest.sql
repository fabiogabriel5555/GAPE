-- Schema support for application-level AES-256-GCM envelopes and HMAC
-- document fingerprints. The Java SensitiveDataMigrationService performs the
-- key-dependent data conversion after this migration; no key appears in SQL.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_sensitive_data_at_rest_schema$$
CREATE PROCEDURE migrate_sensitive_data_at_rest_schema()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'user_account'
          AND column_name = 'document_number_fingerprint'
    ) THEN
        ALTER TABLE user_account
            ADD COLUMN document_number_fingerprint CHAR(64)
                CHARACTER SET ascii COLLATE ascii_bin NULL
                AFTER document_number;
    END IF;

    -- Envelopes have nonce, authentication tag and Base64 expansion. These
    -- MODIFY statements are deliberately idempotent for a repaired rerun.
    ALTER TABLE user_account
        MODIFY COLUMN document_number VARCHAR(512) NULL;
    ALTER TABLE deletion_request
        MODIFY COLUMN reason VARCHAR(512) NULL;
    ALTER TABLE absence_justification
        MODIFY COLUMN reason VARCHAR(512) NOT NULL,
        MODIFY COLUMN decision_notes VARCHAR(768) NULL;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'user_account'
          AND index_name = 'uq_user_account_document_fingerprint'
    ) THEN
        ALTER TABLE user_account
            ADD UNIQUE KEY uq_user_account_document_fingerprint
                (document_type, document_number_fingerprint);
    END IF;
END$$
CALL migrate_sensitive_data_at_rest_schema()$$
DROP PROCEDURE migrate_sensitive_data_at_rest_schema$$
DELIMITER ;
