-- Testes de violacao de FK
-- Each statement must fail.

-- FK to a missing organization
INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, description, ects, duration, type, state
) VALUES
    (9001, 999999, NULL, 'Invalid FK Course', 'CFK', NULL, 60.00, '1', 'short_course', 'active');

-- Session without an existing user
INSERT INTO user_session (
    id_session, id_user, token, state, start_at, last_activity, end_at
) VALUES
    (9008, 999999, 'tok-fk-user-missing', 'active', '2026-03-02 11:00:00', '2026-03-02 11:01:00', NULL);

-- Deletion request without an existing user
INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (9009, 999999, NULL, '2026-03-03 10:00:00', NULL, 'Request without user', 'submitted');

-- Organic unit without an existing organization
INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (9010, 999999, 'UO-X', 'Unidade X', 'UX', 'department', 'active', NULL);

-- Class group without an existing subject
INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    starts_at, ends_at, shift
) VALUES
    (9011, 999999, 30, 300, 3001, 'PRJ-FK', 'onsite', 'active',
     '2026-02-01', '2026-06-30', 'evening');

-- Block without an existing class group
INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    state
) VALUES
    (9012, 999999, 'BLK-FK', 'Block FK', 'Without class group', 1, 'active');

-- Lesson without an existing block
INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (9013, 50, 999999, NULL, 'FK Lesson', NULL, 'onsite', NULL, NULL, 1, 'active',
     '2026-03-05 10:00:00', '2026-03-05 12:00:00');

-- Option without an existing question
INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag
) VALUES
    (9014, 999999, 1, 'Option without question', 0);

-- Attempt without an existing student
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9015, 999999, 90, 1, NULL, 'in_progress', '2026-03-06 10:00:00', NULL);

-- Attempt without an existing assessment
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (9016, 4, 999999, 1, NULL, 'in_progress', '2026-03-06 11:00:00', NULL);

-- Response without an existing attempt
INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (9017, 999999, 100, 'R-FK', NULL, NULL, NULL, NULL);

-- Certificate without an existing course
INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, issued_at, final_grade
) VALUES
    (9018, 999999, 300, 4, 'Course FK Certificate', NULL, 'completion', NULL, NULL, NULL);

-- Certificate without an existing student
INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, issued_at, final_grade
) VALUES
    (9019, 30, 300, 999999, 'Student FK Certificate', NULL, 'completion', NULL, NULL, NULL);

-- FK para permission inexistente
INSERT INTO grant_teacher (id_teacher_user, cod_permission)
VALUES (3, 'PERMISSION_NOT_FOUND');
