-- GAPE - Dados de demonstracao
-- Este script assume que sql/seed/base.sql ja foi executado.

START TRANSACTION;

-- Utilizadores adicionais
INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (6, 'Teacher Two', 'teacher2@gape.local', 'active', 'en-US', 'users/6/profile.webp', '2026-01-02 09:00:00', 'tuRaTV4DOE/Pwph2t8arh4vVHBQfvyHC2vfx4C7u3WM=', 'nNPHYRNVfnUJwJJf1xHP4Q==', NULL, NULL),
    (7, 'Student Two', 'student2@gape.local', 'active', 'pt-PT', 'users/7/profile.webp', '2026-01-02 09:05:00', 'ELOXjF01xl3ZiUslNj30kUZdreo8BYaEit34GOz/z64=', 'ckGB7Z5+GO9rxxuSycfE7Q==', 'RESIDENCE_PERMIT', 'RP-778899');

INSERT INTO teacher_profile (id_user, cod_teacher) VALUES (6, 'TCH-002');
INSERT INTO student_profile (id_user, cod_student) VALUES (7, 'STD-007');

-- Permissoes adicionais nos novos perfis
INSERT INTO grant_teacher (id_teacher_user, cod_permission) VALUES
    (6, 'VIEW_REPORTS');

INSERT INTO grant_student (id_student_user, cod_permission) VALUES
    (7, 'VIEW_REPORTS');

-- Estrutura adicional: nova turma e bloco
INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (51, 40, 30, 'PRJ-PL2', 'online', 'active', 5, 35, '2026-02-01', '2026-06-30', 'morning');

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (61, 51, 'BLK-02', 'Planeamento', 'Segundo bloco da UC', 1, 'restricted', 'active',
     '2026-02-15 00:00:00', '2026-04-15 23:59:59');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (6, 51, 'active', '2026-02-01', NULL);

INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date) VALUES
    (7, 30, 'active', '2026-02-01', NULL);

INSERT INTO enroll_subject (id_student_user, id_subject, state, start_date, end_date) VALUES
    (7, 40, 'active', '2026-02-01', NULL);

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (7, 51, 'active', '2026-02-01', NULL);

-- Conteudos e aula adicional
INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at, updated_at
) VALUES
    (73, 6, 'Video de apoio', 'Video de planeamento da iteracao', 'video', 'https://example.local/video/planning', 'active',
     '2026-02-15 09:00:00', NULL);

INSERT INTO associate_class_group_content (id_class_group, id_content_item, role) VALUES
    (51, 73, 'support');

INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory) VALUES
    (61, 73, 1, 'main', 0);

INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type, provider,
    access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (81, 51, 61, NULL, 'Aula Online 1', 'Aula de planeamento remoto', 'online', 'microsoft_teams',
     'https://teams.example.local/prj', 1, 'active', '2026-02-18 09:00:00', '2026-02-18 11:00:00');

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, state, available_from, available_until
) VALUES
    (93, 40, 61, 'Questionario Online PRJ-PL2', 'Avaliacao da turma online de demonstracao', 'questionnaire', 'online', 'automatic',
     20.00, 9.50, 2, 'active', '2026-02-17 00:00:00', '2026-02-20 23:59:59');

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer, state
) VALUES
    (102, 93, 'Q1', 'Qual o objetivo do sprint planning?', 'single_choice', 1, 1, 20.00, NULL, 'active');

INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag, state
) VALUES
    (114, 102, 1, 'Definir e planear o trabalho do sprint', 1, 'active'),
    (115, 102, 2, 'Encerrar o sprint atual', 0, 'active');

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (142, 81, NULL, 'Evento Aula Online 1', 'Sessao online da turma PRJ-PL2', 'lesson',
     '2026-02-18 09:00:00', '2026-02-18 11:00:00', 0, 1, 20, 'active');

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (6, 142),
    (7, 142);

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (142, 51);

INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (151, 81, 7, 'late', 'automatic', '2026-02-18 09:12:00', '2026-02-18 11:00:00', 'Entrada apos inicio', 'active');

-- Segunda tentativa e registo de nota
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (121, 7, 93, 1, 8.00, 'submitted', '2026-02-18 10:00:00', '2026-02-18 10:12:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (131, 121, 102, 'R1', NULL, NULL, 8.00, '2026-02-18 10:06:00');

INSERT INTO response_option (id_response, id_option) VALUES
    (131, 114);

INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes, state
) VALUES
    (181, 170, 7, 121, 'GR-002', 8.00, 'reproved', '2026-02-12 12:05:00', 'Necessita reforco de estudo', 'active');

-- Mensagens de demonstracao no canal
INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (210, 'Canal Online PRJ-PL2', 'class_group', 'participants', '2026-02-15 09:55:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (6, 210, 'teacher', '2026-02-15 10:00:00', 0, 'active'),
    (7, 210, 'student', '2026-02-15 10:00:00', 0, 'active');

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (224, 210, 6, NULL, 142, 'Preparacao Aula Online', 'Rever material antes da sessao', 'announcement',
     'high', NULL, '2026-02-17 18:00:00', NULL, NULL, '2026-02-17 18:01:00', 'sent');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, delivery_mode, state
) VALUES
    (7, 224, '2026-02-17 18:01:10', '2026-02-17 18:30:00', 'internal', 'read');

-- Certificado adicional
INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template,
    validation_code, issued_at, final_grade, state
) VALUES
    (193, 30, 7, 'Certificado de Participacao', 'Participacao no modulo', 'attendance', 'template-v1',
     'VAL-2026-0002', '2026-07-01 11:00:00', 8.00, 'issued');

INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (193, 170);

-- Log adicional
INSERT INTO activity_log (
    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
    affected_entity_identifier, occurred_at, outcome, source_ip
) VALUES
    (234, 6, NULL, 'SEND', 'message', '224', '2026-02-17 18:01:00', 'success', '127.0.0.1'),
    (235, 7, NULL, 'READ', 'message', '224', '2026-02-17 18:30:00', 'success', '127.0.0.1');

COMMIT;
