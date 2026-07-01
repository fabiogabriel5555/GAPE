-- Valid data for SQL integrity and application rule tests

INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt
) VALUES
    (1, 'Admin User', 'admin@gape.local', 'active', 'pt-PT', 'users/1/profile.webp', '2026-01-01 09:00:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw=='),
    (2, 'Coordinator User', 'coord@gape.local', 'active', 'pt-PT', 'users/2/profile.webp', '2026-01-01 09:05:00', 'jvjSnRgJRoVWPXjTD/9Z6ldwNGCWYF3ufKq7n7BSTtw=', 'PeX6EFsl+vvJMTpvQPbvOg=='),
    (3, 'Teacher User', 'teacher@gape.local', 'active', 'pt-PT', 'users/3/profile.webp', '2026-01-01 09:10:00', 'P+iPeOLXBL7c86xAWsax6ynSRK5lF0izcOJ+xJJ/nt0=', 'NduPZR+nNoURf2lKx3e5Aw=='),
    (4, 'Student User', 'student@gape.local', 'active', 'pt-PT', 'users/4/profile.webp', '2026-01-01 09:15:00', 'Pf6qW2Vh05dgyvwU5XUhaJpgNrWO9Jgz3ashXolzSVU=', 'x0+e4osQuVa/3kxuimgT9Q=='),
    (5, 'Inactive User', 'inactive@gape.local', 'inactive', 'pt-PT', 'users/5/profile.webp', '2026-01-01 09:20:00', 'rIP3h7CjjJIsuECNEYC4kQZ+fsln+oZvem31yw4BnB8=', 'GcaOR1rZz14IGkuD1k5wyQ==');

INSERT INTO administrator_profile (id_user, cod_administrator) VALUES (1, 'ADM-001');
INSERT INTO coordinator_profile (id_user, cod_coordinator) VALUES (2, 'COO-001');
INSERT INTO teacher_profile (id_user, cod_teacher) VALUES (3, 'TCH-001');
INSERT INTO student_profile (id_user, cod_student) VALUES (4, 'STD-001');
INSERT INTO student_profile (id_user, cod_student) VALUES (5, 'STD-002');

INSERT INTO user_session (
    id_session, id_user, token, state, start_at, last_activity, end_at
) VALUES
    (100, 1, 'tok-admin-100', 'active', '2026-01-10 10:00:00', '2026-01-10 10:30:00', NULL);

INSERT INTO permission (cod_permission, name, state) VALUES
    ('MANAGE_ALL', 'Manage All', 'active'),
    ('MANAGE_ORGANIZATION_STRUCTURE', 'Manage Organization Structure', 'active'),
    ('MANAGE_LEARNING', 'Manage Learning', 'active'),
    ('MANAGE_ENROLLMENTS', 'Manage Enrollments', 'active');

INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id) VALUES
    (1, 'MANAGE_ALL', 'GLOBAL', 0);

INSERT INTO grant_coordinator (id_coordinator_user, cod_permission) VALUES
    (2, 'MANAGE_LEARNING');

INSERT INTO grant_teacher (id_teacher_user, cod_permission) VALUES
    (3, 'MANAGE_LEARNING');

INSERT INTO organization (id_organization, name, acronym, photo, type, state) VALUES
    (10, 'GAPE Higher Institute', 'ISG', 'organizations/10/profile.webp', 'educational_institution', 'inactive'),
    (11, 'External Organization', 'ORGX', 'organizations/11/profile.webp', 'training_company', 'inactive'),
    (19, 'Inactive Organization Without Admin', 'OISA', NULL, 'other', 'inactive');

INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date) VALUES
    (1, 10, 'active', '2026-01-01', NULL),
    (1, 11, 'active', '2026-01-01', NULL);

UPDATE organization
SET state = 'active'
WHERE id_organization IN (10, 11);

INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (20, 10, 'DEI', 'Computer Engineering Department', 'DEI', 'department', 'active', NULL),
    (21, 11, 'EXT', 'External Unit', 'EXT', 'section', 'active', NULL),
    (22, 10, 'NPRJ', 'Project Center', 'NPRJ', 'section', 'active', 20);

INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, photo, description, ects, duration, type, state
) VALUES
    (30, 10, 20, 'Computer Engineering', 'LEI', NULL, 'Licenciatura em Computer Engineering', 180.00, '3', 'degree', 'active'),
    (31, 10, 20, 'Data Analysis', 'AD', NULL, 'Data analysis course', 60.00, '1', 'short_course', 'active');

INSERT INTO subject (
    id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
) VALUES
    (40, 10, 'Project', 'PRJ', NULL, 'Project curricular unit', 12.00, 140, 'active'),
    (41, 10, 'Applied Mathematics', 'MAT', NULL, 'Applied mathematics curricular unit', 6.00, 70, 'active');

INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory, state
) VALUES
    (30, 40, 3, 'annual', 1, 'active'),
    (30, 41, 1, 'semester_1', 1, 'active'),
    (31, 41, 1, 'semester_1', 1, 'active');

INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state, start_date, end_date) VALUES
    (2, 40, 'active', '2026-01-15', NULL);

INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (50, 40, 30, 'PRJ-T1', 'onsite', 'active', 5, 30, '2026-02-01', '2026-06-30', 'evening'),
    (52, 41, 30, 'MAT-T1', 'online', 'active', 10, 40, '2026-02-01', '2026-06-30', 'morning');

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (60, 50, 'BLK-01', 'Introduction', 'First block', 1, 'open', 'active', '2026-02-01 00:00:00', '2026-03-01 23:59:59'),
    (62, 52, 'BLK-02', 'Reviews', 'Review block', 1, 'scheduled', 'active', '2026-03-01 00:00:00', '2026-04-01 23:59:59');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (3, 50, 'active', '2026-02-01', NULL);

INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date) VALUES
    (4, 30, 'active', '2026-02-01', NULL);

INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date) VALUES
    (4, 30, 40, 'active', '2026-02-01', NULL);

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (4, 50, 'active', '2026-02-01', NULL);

INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at
) VALUES
    (70, 3, 'Course Unit Guide', 'Introductory guide', 'pdf', 'contents/guide-prj.pdf', 'active', '2026-02-02 10:00:00'),
    (71, 3, 'Notes', 'Support text', 'text', 'contents/full-lesson-text.txt', 'active', '2026-02-03 10:00:00'),
    (72, 3, 'Archived Content', 'Must not be reused in new active contexts', 'pdf', 'contents/legacy.pdf', 'inactive', '2026-01-15 08:00:00');

INSERT INTO associate_class_group_content (id_class_group, id_content_item, role) VALUES
    (50, 70, 'support');

INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory) VALUES
    (60, 70, 1, 'main', 1);

INSERT INTO physical_room (
    cod_physical_room, id_organization, id_organic_unit, name, description, capacity, location, state
) VALUES
    ('SALA-A1', 10, 20, 'Sala A1', 'Main laboratory', 25, 'Building A', 'active'),
    ('SALA-X1', 11, 21, 'Sala X1', 'External organization room', 12, 'Building X', 'active');

INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (80, 50, 60, 'SALA-A1', 'Lesson 1', 'Opening session', 'onsite', NULL, NULL, 1, 'active',
     '2026-02-05 18:00:00', '2026-02-05 20:00:00');

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, final_grade_weight, attempts_limit, state, available_from, available_until
) VALUES
    (90, 40, 60, 'Form 1', 'Formative assessment', 'form', 'online', 'automatic',
     20.00, 9.50, 50.00, 2, 'active', '2026-02-10 00:00:00', '2026-02-20 23:59:59'),
    (91, 41, 62, 'Form 2', 'Mathematics assessment', 'form', 'online', 'automatic',
     20.00, 10.00, 100.00, 1, 'active', '2026-03-02 00:00:00', '2026-03-10 23:59:59'),
    (92, 40, NULL, 'Final Exam', 'Final subject assessment', 'exam', 'onsite', 'manual',
     20.00, 9.50, 50.00, 1, 'draft', '2026-06-20 09:00:00', '2026-06-20 11:00:00');

INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (92, 50);

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer
) VALUES
    (100, 90, 'Q1', 'What is the definition of a functional requirement?', 'single_choice', 1, 1, 10.00, NULL),
    (101, 91, 'Q1', 'What is 2 + 2?', 'single_choice', 1, 1, 20.00, NULL);

INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag
) VALUES
    (110, 100, 1, 'Correct option', 1),
    (111, 100, 2, 'Incorrect option', 0),
    (112, 101, 1, '4', 1),
    (113, 101, 2, '5', 0);

INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (4, 90, 'active');

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (120, 4, 90, 1, NULL, 'submitted', '2026-02-11 10:00:00', '2026-02-11 10:10:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (130, 120, 100, 'R1', NULL, NULL, 10.00, '2026-02-11 10:05:00');

INSERT INTO response_option (id_response, id_option) VALUES
    (130, 110);

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (140, 80, NULL, 'Evento Lesson 1', 'Scheduled lesson', 'lesson',
     '2026-02-05 18:00:00', '2026-02-05 20:00:00', 0, 1, 30, 'active');

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (3, 140),
    (4, 140);

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (140, 50);

INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (150, 80, 4, 'late', 'manual', '2026-02-05 18:01:00', '2026-02-05 20:00:00', NULL, 'active');

INSERT INTO absence_justification (
    id_absence_justification, id_attendance_record, id_user_student_submitter, id_user_processor,
    submitted_at, reason, attachment, processed_at, decision_notes, state
) VALUES
    (160, 150, 4, NULL, '2026-02-06 09:00:00', 'Transport delay', NULL, NULL, NULL, 'submitted');

INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, title, type, released_at, state
) VALUES
    (170, 40, 'Midterm Grade Sheet', 'partial', NULL, 'draft');

INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (170, 50);

INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (170, 90, 100.00);

INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, state, recorded_at, notes
) VALUES
    (180, 170, 4, 120, 'GR-001', 10.00, 'approved', 'archived', '2026-02-10 12:00:00',
     'Archived seed record; active final grades are calculated automatically only after every weighted assessment has a corrected score.');

INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template, validation_code, issued_at, state, revoked_at, final_grade
) VALUES
    (191, 30, 4, 'Participation Certificate', 'Participation completed', 'attendance', 'template-v1',
     NULL, NULL, 'draft', NULL, NULL),
    (192, 31, 4, 'Alternative Certificate', 'Used for inconsistency tests', 'completion', 'template-v1',
     'VAL-2026-0005', '2026-07-01 11:30:00', 'draft', NULL, NULL);

INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (191, 170);

INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (190, 'PRJ-T1 Class Group', 'class_group', 'participants', '2026-02-01 08:00:00', 'active'),
    (191, 'Secondary Channel', 'class_group', 'participants', '2026-02-01 08:05:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (3, 190, 'teacher', '2026-02-01 08:00:00', 0, 'active'),
    (4, 190, 'student', '2026-02-01 08:00:00', 0, 'active'),
    (3, 191, 'teacher', '2026-02-01 08:05:00', 0, 'active');

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (222, 190, 3, NULL, 140, 'Lesson 1 Preparation', 'Review the material before the session', 'announcement',
     'high', NULL, '2026-02-04 18:00:00', NULL, NULL, '2026-02-04 18:01:00', 'sent');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, delivery_mode, state
) VALUES
    (4, 222, '2026-02-04 18:01:10', '2026-02-04 18:30:00', 'internal', 'read');

INSERT INTO activity_log (
    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
    affected_entity_identifier, occurred_at, outcome, source_ip
) VALUES
    (232, 1, 100, 'MANAGE_USERS', 'user_account', '4', '2026-02-12 13:00:00', 'success', '127.0.0.1');
