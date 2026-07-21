-- V032 sized envelopes for ordinary ASCII input. Existing validation permits
-- Portuguese/Unicode text, whose UTF-8 representation can be longer; retain
-- the clear-text limits while safely allowing AES-GCM/Base64 expansion.
ALTER TABLE deletion_request
    MODIFY COLUMN reason VARCHAR(2048) NULL;

ALTER TABLE absence_justification
    MODIFY COLUMN reason VARCHAR(2048) NOT NULL,
    MODIFY COLUMN decision_notes VARCHAR(4096) NULL;
