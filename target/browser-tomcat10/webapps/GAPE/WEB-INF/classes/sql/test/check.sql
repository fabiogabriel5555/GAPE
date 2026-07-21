-- Testes de violacao de CHECK
-- Each statement must fail.

-- Invalid state in User
INSERT INTO user_account (
    id_user, name, email, state, language, created_at, credential_hash, credential_salt
) VALUES
    (9004, 'State Invalido', 'state-invalid@gape.local', 'pending', 'pt-PT', '2026-03-01 12:00:00', 'h_state', 's_state');

-- Integrate_Subject exige curricular_year e term em conjunto
INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory
) VALUES
    (31, 40, 1, NULL, 1);

-- min_students cannot exceed max_students when both are set
INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (9026, 40, 30, 300, 3001, 'PRJ-RANGE', 'onsite', 'active', 10, 5, '2026-02-01', '2026-06-30', 'evening');

-- ends_at cannot be earlier than starts_at when both are set
INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (9027, 40, 30, 300, 3001, 'PRJ-DATES', 'onsite', 'active', 5, 20, '2026-06-30', '2026-02-01', 'evening');

-- Content_Block accepts only supported states
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    state
) VALUES
    (9028, 50, 'BLK-BAD-STATE', 'Block with invalid state', NULL, 2, 'scheduled');

-- capacity > 0 quando preenchido
INSERT INTO physical_room (
    cod_physical_room, id_organization, id_organic_unit, name, description, capacity, location, state
) VALUES
    ('SALA-ZERO', 10, 20, 'Sala Zero', NULL, 0, 'Edificio A', 'active');

-- Deletion final exige processed_at
INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (9029, 4, 1, '2026-03-03 10:00:00', NULL, 'Request without final processing', 'approved');

-- Deletion processed_at exige processor_admin_user_id
INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (9030, 4, NULL, '2026-03-03 10:00:00', '2026-03-03 12:00:00', 'Request without processor', 'under_review');

-- Invalid type in Question
INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer
) VALUES
    (9031, 90, 'QX', 'Invalid question', 'unsupported', 2, 1, 5.00, NULL);

-- Invalid Assessment mode
INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, state, available_from, available_until
) VALUES
    (9036, 40, 60, NULL, 'Invalid Assessment Mode', NULL, 'form', 'hybrid', 'automatic',
     20.00, 10.00, 1, 'active', '2026-03-01 00:00:00', '2026-03-10 00:00:00');

-- Invalid state in Attempt
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9032, 4, 90, 2, NULL, 'done', '2026-02-11 11:00:00', NULL);

-- reminder_enabled=true exige reminder_minutes_before
INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (9033, NULL, NULL, 'Event without reminder', NULL, 'meeting',
     '2026-03-05 10:00:00', '2026-03-05 11:00:00', 0, 1, NULL, 'active');

-- Invalid source in Attendance_Record
INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (9034, 80, 4, 'present', 'scanner', '2026-02-05 18:01:00', '2026-02-05 20:00:00', NULL, 'active');

-- Absence_Justification aprovada exige processed_at e processor
INSERT INTO absence_justification (
    id_absence_justification, id_attendance_record, id_user_student_submitter, id_user_processor,
    submitted_at, reason, attachment, processed_at, decision_notes, state
) VALUES
    (9035, 150, 4, NULL, '2026-02-06 10:00:00', 'Invalid request', NULL, NULL, NULL, 'approved');

-- Grade_Sheet with invalid state
INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, id_course_occurrence, title, type, released_at, state
) VALUES
    (9036, 40, 300, 'Invalid Grade Sheet', 'partial', NULL, 'active');

-- weight tem de estar entre 0 e 100
INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 92, -1.00);

-- Certificate type must be valid
INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, issued_at, final_grade
) VALUES
    (9037, 30, 300, 4, 'Invalid Certificate', NULL, 'invalid', NULL, NULL, NULL);

-- Issued Certificate requires validation code, issued_at and final_grade
INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template,
    validation_code, issued_at, state, final_grade
) VALUES
    (9040, 30, 300, 5, 'Issued Certificate Without Code', NULL, 'completion', NULL,
     NULL, '2026-07-01 10:00:00', 'issued', 15.00);

-- Draft Certificate cannot expose issued fields
INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template,
    validation_code, issued_at, state, final_grade
) VALUES
    (9041, 31, 310, 5, 'Draft Certificate With Code', NULL, 'completion', NULL,
     'CERT-INVALID-DRAFT', NULL, 'draft', NULL);

-- Message scheduled exige scheduled_at
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9038, 190, 3, NULL, NULL, 'Scheduled Message', 'Body', 'text', 'normal', NULL,
     '2026-03-01 10:00:00', NULL, NULL, NULL, 'scheduled');

-- Message attachment exige attachment
INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (9039, 190, 3, NULL, NULL, 'Message with Attachment', 'Body', 'attachment', 'normal', NULL,
     '2026-03-01 10:00:00', NULL, NULL, '2026-03-01 10:05:00', 'sent');
