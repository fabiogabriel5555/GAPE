-- Testes de violacao de UNIQUE
-- Each statement must fail.

-- UNIQUE em user_account.email
INSERT INTO user_account (
    id_user, name, email, state, language, created_at, credential_hash, credential_salt
) VALUES
    (9002, 'Email Duplicado', 'admin@gape.local', 'active', 'pt-PT', '2026-03-01 11:00:00', 'h_dup', 's_dup');

-- UNIQUE em user_session.token
INSERT INTO user_session (
    id_session, id_user, token, state, start_at, last_activity, end_at
) VALUES
    (9003, 1, 'tok-admin-100', 'active', '2026-03-01 11:05:00', '2026-03-01 11:06:00', NULL);

-- Documento unico quando preenchido
INSERT INTO user_account (
    id_user, name, email, state, language, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (9020, 'Doc A', 'doc-a@gape.local', 'active', 'pt-PT', '2026-03-01 14:00:00', 'h_doc_a', 's_doc_a', 'PASSPORT', 'AA123456'),
    (9021, 'Doc B', 'doc-b@gape.local', 'active', 'pt-PT', '2026-03-01 14:01:00', 'h_doc_b', 's_doc_b', 'PASSPORT', 'AA123456');

-- Block order must be unique in the same class group
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (9022, 50, 'BLK-UNQ-ORDER', 'Bloco Ordem Duplicada', NULL, 1, 'open', 'active', NULL, NULL);

-- Only one non-revoked certificate is allowed per course and student
INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template,
    validation_code, issued_at, state, revoked_at, final_grade
) VALUES
    (9023, 30, 4, 'Duplicate Active Certificate', NULL, 'completion', 'template-v1',
     'VAL-UNQ-ACTIVE-CERT', '2026-07-02 11:00:00', 'issued', NULL, 10.00);
