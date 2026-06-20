-- Testes de violacao de CHECK
-- Cada statement deve falhar.

-- state invalido em User
INSERT INTO user_account (
    id_user, name, email, state, language, created_at, credential_hash, credential_salt
) VALUES
    (9004, 'State Invalido', 'state-invalid@gape.local', 'pending', 'pt-PT', '2026-03-01 12:00:00', 'h_state', 's_state');

-- Integrate_Subject exige curricular_year e term em conjunto
INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory, state
) VALUES
    (31, 40, 1, NULL, 1, 'active');

-- min_students nao pode exceder max_students quando ambos estao preenchidos
INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (9026, 40, 30, 'PRJ-RANGE', 'onsite', 'active', 10, 5, '2026-02-01', '2026-06-30', 'evening');

-- ends_at nao pode ser anterior a starts_at quando ambos estao preenchidos
INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (9027, 40, 30, 'PRJ-DATES', 'onsite', 'active', 5, 20, '2026-06-30', '2026-02-01', 'evening');

-- bloco scheduled exige available_from
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (9028, 50, 'BLK-SCHED', 'Bloco sem data', NULL, 2, 'scheduled', 'active', NULL, NULL);

-- capacity > 0 quando preenchido
INSERT INTO physical_room (
    cod_physical_room, id_organization, id_organic_unit, name, description, capacity, location, state
) VALUES
    ('SALA-ZERO', 10, 20, 'Sala Zero', NULL, 0, 'Edificio A', 'active');

-- Deletion final exige processed_at
INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (9029, 4, 1, '2026-03-03 10:00:00', NULL, 'Pedido sem processamento final', 'approved');

-- Deletion processed_at exige processor_admin_user_id
INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (9030, 4, NULL, '2026-03-03 10:00:00', '2026-03-03 12:00:00', 'Pedido sem processor', 'under_review');

-- state invalido em Question
INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer, state
) VALUES
    (9031, 90, 'QX', 'Pergunta invalida', 'single_choice', 2, 1, 5.00, NULL, 'pending');

-- state invalido em Attempt
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9032, 4, 90, 2, NULL, 'done', '2026-02-11 11:00:00', NULL);

-- reminder_enabled=true exige reminder_minutes_before
INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (9033, NULL, NULL, 'Evento sem reminder', NULL, 'meeting',
     '2026-03-05 10:00:00', '2026-03-05 11:00:00', 0, 1, NULL, 'active');

-- source invalida em Attendance_Record
INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (9034, 80, 4, 'present', 'scanner', '2026-02-05 18:01:00', '2026-02-05 20:00:00', NULL, 'active');

-- Absence_Justification aprovada exige processed_at e processor
INSERT INTO absence_justification (
    id_absence_justification, id_attendance_record, id_user_student_submitter, id_user_processor,
    submitted_at, reason, attachment, processed_at, decision_notes, state
) VALUES
    (9035, 150, 4, NULL, '2026-02-06 10:00:00', 'Pedido invalido', NULL, NULL, NULL, 'approved');

-- Grade_Sheet com state invalido
INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, title, type, released_at, state
) VALUES
    (9036, 40, 'Pauta Invalida', 'partial', NULL, 'active');

-- weight tem de ser > 0
INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 92, 0.00);

-- Certificate issued exige validation_code e issued_at
INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template, validation_code, issued_at, final_grade, state
) VALUES
    (9037, 30, 4, 'Certificado Invalido', NULL, 'completion', NULL, NULL, NULL, NULL, 'issued');

-- Message scheduled exige scheduled_at
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9038, 190, 3, NULL, NULL, 'Mensagem Agendada', 'Corpo', 'text', 'normal', NULL,
     '2026-03-01 10:00:00', NULL, NULL, NULL, 'scheduled');

-- Message attachment exige attachment
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9039, 190, 3, NULL, NULL, 'Mensagem com Anexo', 'Corpo', 'attachment', 'normal', NULL,
     '2026-03-01 10:00:00', NULL, NULL, '2026-03-01 10:05:00', 'sent');

-- delivery_mode invalido em Receive_Message
INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, delivery_mode, state
) VALUES
    (4, 222, '2026-03-01 10:01:00', NULL, 'in_app', 'delivered');
