-- Testes de violacao de UNIQUE
-- Cada statement deve falhar.

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
    (9020, 'Doc A', 'doc-a@gape.local', 'active', 'pt-PT', '2026-03-01 14:00:00', 'h_doc_a', 's_doc_a', 'passport', 'AA123456'),
    (9021, 'Doc B', 'doc-b@gape.local', 'active', 'pt-PT', '2026-03-01 14:01:00', 'h_doc_b', 's_doc_b', 'passport', 'AA123456');

-- Ordem do bloco unica na mesma turma
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (9022, 50, 'BLK-UNQ-ORDER', 'Bloco Ordem Duplicada', NULL, 1, 'open', 'active', NULL, NULL);
