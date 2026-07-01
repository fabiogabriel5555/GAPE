-- Testes de restricoes aplicacionais refletidas em triggers SQL
-- Each statement must fail.

-- Active organization cannot be inserted directly without a previous administrator assignment
INSERT INTO organization (id_organization, name, acronym, type, state)
VALUES (9200, 'Active Organization Without Admin', 'OASA', 'company', 'active');

-- Inactive organization without an administrator cannot be activated
UPDATE organization
SET state = 'active'
WHERE id_organization = 19;

-- Manage_Organization accepts only controlled states
INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date)
VALUES (1, 11, 'invalid', '2026-01-01', NULL);

-- Last active assignment cannot be removed from an active organization
DELETE FROM manage_organization
WHERE id_admin_user = 1
  AND id_organization = 10;

-- System cannot be left without an active administrator with global MANAGE_ALL
DELETE FROM grant_administrator
WHERE id_admin_user = 1
  AND cod_permission = 'MANAGE_ALL'
  AND context_type = 'GLOBAL'
  AND context_id = 0;

-- Organic_Unit cannot create a long cycle in the hierarchy
UPDATE organic_unit
SET parent_organic_unit_id = 22
WHERE id_organic_unit = 20;

-- Organization with dependencies cannot be removed by direct SQL
DELETE FROM organization
WHERE id_organization = 10;

-- Organic_Unit with dependencies cannot be removed by direct SQL
DELETE FROM organic_unit
WHERE id_organic_unit = 20;

-- Organic_Unit pai tem de pertencer a mesma Organization
INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (9100, 10, 'UO-X', 'Unit with invalid parent', 'UOX', 'section', 'active', 21);

-- Course Organic_Unit tem de pertencer a mesma Organization
INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, description, ects, duration, type, state
) VALUES
    (9101, 10, 21, 'Invalid Course', 'CI', NULL, 60.00, '1', 'short_course', 'active');

-- Class_Group exige Subject integrada no Course
INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (9102, 40, 31, 'CG-NAO-INTEGRADA', 'onsite', 'active', 5, 20, '2026-02-01', '2026-06-30', 'mixed');

-- Enroll_Subject must be covered by the complete course enrollment period
INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date)
VALUES (4, 30, 41, 'active', '2026-01-01', NULL);

-- Enroll_Class_Group requires active enrollment in the class group subject
INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date)
VALUES (4, 52, 'active', '2026-02-01', NULL);

-- Class_Group cannot lower max_students below active enrollments
UPDATE class_group
SET min_students = 0,
    max_students = 0
WHERE id_class_group = 50;

-- Active Content_Block order must be unique in the class group
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

-- Content_Item source is required for structured formats
INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at
) VALUES
    (9103, 3, 'PDF Without Source', NULL, 'pdf', NULL, 'active', '2026-03-01 09:00:00');

-- Lesson online exige access_url
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9104, 50, 60, NULL, 'Invalid Online Lesson', NULL, 'online', NULL, NULL, 1, 'active',
     '2026-03-05 10:00:00', '2026-03-05 11:00:00');

-- Lesson onsite exige Physical_Room
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9105, 50, 60, NULL, 'In-Person Lesson Without Room', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 12:00:00', '2026-03-05 13:00:00');

-- Lesson cannot use a block from another class group
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9106, 50, 62, 'SALA-A1', 'Lesson with wrong block', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 14:00:00', '2026-03-05 15:00:00');

-- Lesson cannot use a room from another organization
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9107, 50, 60, 'SALA-X1', 'Lesson with external room', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 16:00:00', '2026-03-05 17:00:00');

-- Lesson cannot overlap another active lesson in the same room
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9108, 50, 60, 'SALA-A1', 'Overlapping lesson', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-02-05 19:00:00', '2026-02-05 21:00:00');

-- Questionnaire exige Content_Block
INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, state, available_from, available_until
) VALUES
    (9109, 40, NULL, 'Form without block', NULL, 'form', 'online', 'automatic',
     20.00, 10.00, 1, 'active', '2026-03-01 00:00:00', '2026-03-10 00:00:00');

-- Assessment subject must match the Content_Block subject
INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, state, available_from, available_until
) VALUES
    (9110, 40, 62, 'Assessment incoerente', NULL, 'form', 'online', 'automatic',
     20.00, 10.00, 1, 'active', '2026-03-01 00:00:00', '2026-03-10 00:00:00');

-- Assessment_Class_Group only applies to subject-level exams
INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (90, 50);

-- Assessment_Class_Group Class_Group must match the Assessment subject
INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (92, 52);

-- Attempt cannot exceed attempts_limit
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9111, 4, 90, 3, NULL, 'in_progress', '2026-02-12 10:00:00', NULL);

-- Attempt requires active enrollment in the assessment context
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

-- single_choice does not allow a second option in the same response
INSERT INTO response_option (id_response, id_option) VALUES
    (130, 111);

-- Schedule_Event ligado a Lesson tem de usar tipo lesson
INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (9114, 80, NULL, 'Evento inconsistente', NULL, 'meeting',
     '2026-02-05 18:00:00', '2026-02-05 20:00:00', 0, 0, NULL, 'active');

-- Lesson Schedule_Event must have the same period
INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (9115, 80, NULL, 'Event outside the period', NULL, 'lesson',
     '2026-02-05 17:00:00', '2026-02-05 20:00:00', 0, 0, NULL, 'active');

-- Attendance_Record requires active enrollment in the lesson class group
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

-- Grade_Sheet and Class_Group must have the same subject
INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (170, 52);

-- Grade_Sheet and Class_Group subject validation also applies on update
UPDATE associate_grade_sheet_class_group
SET id_class_group = 52
WHERE id_grade_sheet = 170
  AND id_class_group = 50;

-- Based_On_Assessment requires context consistent with Grade_Sheet
INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 91, 10.00);

-- Based_On_Assessment weight must stay in the allowed 0..100 interval
INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 92, 100.01);

-- Grade_Record with another student attempt
INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes
) VALUES
    (9118, 170, 5, 120, 'GR-ERR-STUDENT', 10.00, 'approved', '2026-02-12 13:00:00', NULL);

-- Grade_Record does not allow two records for the same grade sheet and student
INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes
) VALUES
    (9119, 170, 4, NULL, 'GR-ERR-DUP', 9.00, 'approved', '2026-02-12 13:05:00', NULL);

-- Certificate Course tem de integrar a Subject da Grade_Sheet
INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (192, 170);

-- Certificate Grade_Sheet subject validation also applies on update
UPDATE based_on_grade_sheet_certificate
SET id_certificate = 192
WHERE id_certificate = 191
  AND id_grade_sheet = 170;

-- Message sender tem de participar no Channel
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9120, 190, 2, NULL, NULL, 'Message without participant', 'Body', 'text', 'normal', NULL,
     '2026-03-01 11:00:00', NULL, NULL, '2026-03-01 11:01:00', 'sent');

-- Reply must belong to the same channel as the parent message
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9121, 191, 3, 222, NULL, 'Resposta noutro canal', 'Body', 'comment', 'normal', NULL,
     '2026-03-01 11:10:00', NULL, NULL, '2026-03-01 11:11:00', 'sent');

-- delivery_mode email requires an active user
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
