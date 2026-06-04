-- Testes de violacao de FK
-- Cada statement deve falhar.

-- FK para organization inexistente
INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, description, ects, duration, type, state
) VALUES
    (9001, 999999, NULL, 'Curso FK invalida', 'CFK', NULL, 60.00, '1y', 'short_course', 'active');

-- Sessao sem utilizador existente
INSERT INTO user_session (
    id_session, id_user, token, state, start_at, last_activity, end_at
) VALUES
    (9008, 999999, 'tok-fk-user-missing', 'active', '2026-03-02 11:00:00', '2026-03-02 11:01:00', NULL);

-- Pedido de eliminacao sem utilizador existente
INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (9009, 999999, NULL, '2026-03-03 10:00:00', NULL, 'Pedido sem user', 'submitted');

-- Unidade organica sem organizacao existente
INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (9010, 999999, 'UO-X', 'Unidade X', 'UX', 'department', 'active', NULL);

-- Turma sem disciplina existente
INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state
) VALUES
    (9011, 999999, 30, 'PRJ-FK', 'onsite', 'active');

-- Bloco sem turma existente
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (9012, 999999, 'BLK-FK', 'Bloco FK', 'Sem turma', 1, 'open', 'active', NULL, NULL);

-- Aula sem bloco existente
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9013, 50, 999999, NULL, 'Aula FK', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 10:00:00', '2026-03-05 12:00:00');

-- Opcao sem pergunta existente
INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag, state
) VALUES
    (9014, 999999, 1, 'Opcao sem pergunta', 0, 'active');

-- Tentativa sem aluno existente
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9015, 999999, 90, 1, NULL, 'in_progress', '2026-03-06 10:00:00', NULL);

-- Tentativa sem avaliacao existente
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9016, 4, 999999, 1, NULL, 'in_progress', '2026-03-06 11:00:00', NULL);

-- Resposta sem tentativa existente
INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (9017, 999999, 100, 'R-FK', NULL, NULL, NULL, NULL);

-- Certificado sem curso existente
INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template, validation_code, issued_at, final_grade, state
) VALUES
    (9018, 999999, 4, 'Cert FK Curso', NULL, 'completion', NULL, 'CERT-FK-COURSE', NULL, NULL, 'draft');

-- Certificado sem aluno existente
INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template, validation_code, issued_at, final_grade, state
) VALUES
    (9019, 30, 999999, 'Cert FK Aluno', NULL, 'completion', NULL, 'CERT-FK-STUDENT', NULL, NULL, 'draft');

-- FK para permission inexistente
INSERT INTO grant_teacher (id_teacher_user, cod_permission)
VALUES (3, 'PERMISSION_NOT_FOUND');
