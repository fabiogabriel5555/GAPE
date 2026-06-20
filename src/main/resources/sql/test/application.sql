-- Testes de restricoes aplicacionais refletidas em triggers SQL
-- Cada statement deve falhar.

-- Organization ativa nao pode ser inserida diretamente sem atribuicao previa de administrador
INSERT INTO organization (id_organization, name, acronym, type, state)
VALUES (9200, 'Organizacao Ativa Sem Admin', 'OASA', 'company', 'active');

-- Organization inativa sem administrador nao pode ser ativada
UPDATE organization
SET state = 'active'
WHERE id_organization = 11;

-- Manage_Organization aceita apenas estados controlados
INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date)
VALUES (1, 11, 'invalid', '2026-01-01', NULL);

-- Ultima atribuicao ativa nao pode ser removida de Organization ativa
DELETE FROM manage_organization
WHERE id_admin_user = 1
  AND id_organization = 10;

-- Sistema nao pode ficar sem Administrador ativo com MANAGE_ALL global
DELETE FROM grant_administrator
WHERE id_admin_user = 1
  AND cod_permission = 'MANAGE_ALL'
  AND context_type = 'GLOBAL'
  AND context_id = 0;

-- Organic_Unit nao pode criar ciclo longo na hierarquia
UPDATE organic_unit
SET parent_organic_unit_id = 22
WHERE id_organic_unit = 20;

-- Organization com dependencias nao pode ser removida por SQL direto
DELETE FROM organization
WHERE id_organization = 10;

-- Organic_Unit com dependencias nao pode ser removida por SQL direto
DELETE FROM organic_unit
WHERE id_organic_unit = 20;

-- Organic_Unit pai tem de pertencer a mesma Organization
INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (9100, 10, 'UO-X', 'Unidade com pai invalido', 'UOX', 'section', 'active', 21);

-- Course Organic_Unit tem de pertencer a mesma Organization
INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, description, ects, duration, type, state
) VALUES
    (9101, 10, 21, 'Curso Invalido', 'CI', NULL, 60.00, '1', 'short_course', 'active');

-- Class_Group exige Subject integrada no Course
INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (9102, 40, 31, 'CG-NAO-INTEGRADA', 'onsite', 'active', 5, 20, '2026-02-01', '2026-06-30', 'mixed');

-- Enroll_Subject tem de estar coberta pelo periodo completo da inscricao no Course
INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date)
VALUES (4, 30, 41, 'active', '2026-01-01', NULL);

-- Enroll_Class_Group exige inscricao ativa na disciplina da turma
INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date)
VALUES (4, 52, 'active', '2026-02-01', NULL);

-- Class_Group nao pode baixar max_students abaixo das inscricoes ativas
UPDATE class_group
SET min_students = 0,
    max_students = 0
WHERE id_class_group = 50;

-- Ordem de Content_Block ativa tem de ser unica na turma
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (9124, 50, 'BLK-ACTIVE-ORDER', 'Ordem ativa duplicada', NULL, 1, 'open', 'active', NULL, NULL);

-- Physical_Room Organic_Unit tem de pertencer a mesma Organization
INSERT INTO physical_room (
    cod_physical_room, id_organization, id_organic_unit, name, description, capacity, location, state
) VALUES
    ('SALA-MISMATCH', 10, 21, 'Sala Invalida', NULL, 20, 'Edificio A', 'active');

-- Content_Item source obrigatoria para formatos estruturados
INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at
) VALUES
    (9103, 3, 'PDF Sem Fonte', NULL, 'pdf', NULL, 'active', '2026-03-01 09:00:00');

-- Lesson online exige access_url
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9104, 50, 60, NULL, 'Aula Online Invalida', NULL, 'online', NULL, NULL, 1, 'active',
     '2026-03-05 10:00:00', '2026-03-05 11:00:00');

-- Lesson onsite exige Physical_Room
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9105, 50, 60, NULL, 'Aula Presencial Sem Sala', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 12:00:00', '2026-03-05 13:00:00');

-- Lesson nao pode usar bloco de outra turma
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9106, 50, 62, 'SALA-A1', 'Aula com bloco errado', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 14:00:00', '2026-03-05 15:00:00');

-- Lesson nao pode usar sala de outra Organization
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9107, 50, 60, 'SALA-X1', 'Aula com sala externa', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 16:00:00', '2026-03-05 17:00:00');

-- Lesson nao pode sobrepor outra aula ativa na mesma sala
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9108, 50, 60, 'SALA-A1', 'Aula sobreposta', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-02-05 19:00:00', '2026-02-05 21:00:00');

-- Questionnaire exige Content_Block
INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, state, available_from, available_until
) VALUES
    (9109, 40, NULL, 'Questionario sem bloco', NULL, 'questionnaire', 'online', 'automatic',
     20.00, 10.00, 1, 'active', '2026-03-01 00:00:00', '2026-03-10 00:00:00');

-- Subject da Assessment tem de coincidir com Subject do Content_Block
INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, state, available_from, available_until
) VALUES
    (9110, 40, 62, 'Assessment incoerente', NULL, 'questionnaire', 'online', 'automatic',
     20.00, 10.00, 1, 'active', '2026-03-01 00:00:00', '2026-03-10 00:00:00');

-- Attempt nao pode ultrapassar attempts_limit
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9111, 4, 90, 3, NULL, 'in_progress', '2026-02-12 10:00:00', NULL);

-- Attempt exige inscricao ativa no contexto da Assessment
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9112, 4, 91, 1, NULL, 'in_progress', '2026-03-03 10:00:00', NULL);

-- Response exige Question da mesma Assessment da Attempt
INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (9113, 120, 101, 'R-ERR', NULL, NULL, NULL, NULL);

-- Response_Option exige Option da mesma Question
INSERT INTO response_option (id_response, id_option) VALUES
    (130, 112);

-- single_choice nao permite segunda opcao na mesma resposta
INSERT INTO response_option (id_response, id_option) VALUES
    (130, 111);

-- Schedule_Event ligado a Lesson tem de usar tipo lesson
INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (9114, 80, NULL, 'Evento inconsistente', NULL, 'meeting',
     '2026-02-05 18:00:00', '2026-02-05 20:00:00', 0, 0, NULL, 'active');

-- Schedule_Event da Lesson tem de ter o mesmo periodo
INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (9115, 80, NULL, 'Evento fora do periodo', NULL, 'lesson',
     '2026-02-05 17:00:00', '2026-02-05 20:00:00', 0, 0, NULL, 'active');

-- Attendance_Record exige inscricao ativa na turma da aula
INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (9116, 80, 5, 'present', 'manual', '2026-02-05 18:01:00', '2026-02-05 20:00:00', NULL, 'active');

-- Absence_Justification exige o mesmo Student do Attendance_Record
INSERT INTO absence_justification (
    id_absence_justification, id_attendance_record, id_user_student_submitter, id_user_processor,
    submitted_at, reason, attachment, processed_at, decision_notes, state
) VALUES
    (9117, 150, 5, NULL, '2026-02-06 09:30:00', 'Submissor errado', NULL, NULL, NULL, 'submitted');

-- Grade_Sheet e Class_Group devem ter a mesma Subject
INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (170, 52);

-- Based_On_Assessment exige contexto coerente com Grade_Sheet
INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 91, 10.00);

-- Soma de pesos nao pode exceder 100
INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 92, 10.00);

-- Grade_Record com Attempt de outro Student
INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes, state
) VALUES
    (9118, 170, 5, 120, 'GR-ERR-STUDENT', 10.00, 'approved', '2026-02-12 13:00:00', NULL, 'active');

-- Grade_Record nao permite dois registos ativos para a mesma pauta e aluno
INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes, state
) VALUES
    (9119, 170, 4, NULL, 'GR-ERR-DUP', 9.00, 'approved', '2026-02-12 13:05:00', NULL, 'active');

-- Certificate Course tem de integrar a Subject da Grade_Sheet
INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (192, 170);

-- Message sender tem de participar no Channel
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9120, 190, 2, NULL, NULL, 'Mensagem sem participante', 'Corpo', 'text', 'normal', NULL,
     '2026-03-01 11:00:00', NULL, NULL, '2026-03-01 11:01:00', 'sent');

-- Reply tem de pertencer ao mesmo Channel da mensagem pai
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9121, 191, 3, 222, NULL, 'Resposta noutro canal', 'Corpo', 'comment', 'normal', NULL,
     '2026-03-01 11:10:00', NULL, NULL, '2026-03-01 11:11:00', 'sent');

-- delivery_mode email exige utilizador ativo
INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, delivery_mode, state
) VALUES
    (5, 222, '2026-03-01 11:30:00', NULL, 'email', 'delivered');

-- Activity_Log Session tem de pertencer ao mesmo User
INSERT INTO activity_log (
    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
    affected_entity_identifier, occurred_at, outcome, source_ip
) VALUES
    (9122, 2, 100, 'VIEW', 'message', '222', '2026-03-01 11:45:00', 'denied', '127.0.0.1');
