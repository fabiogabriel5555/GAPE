-- GAPE - Demo data
-- This script assumes that sql/seed/base.sql has already been executed.

START TRANSACTION;

-- Additional users
INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (6, 'Teacher Two', 'teacher2@gape.local', 'active', 'en-US', 'users/6/profile.webp', '2026-01-02 09:00:00', 'tuRaTV4DOE/Pwph2t8arh4vVHBQfvyHC2vfx4C7u3WM=', 'nNPHYRNVfnUJwJJf1xHP4Q==', NULL, NULL),
    (7, 'Student Two', 'student2@gape.local', 'active', 'pt-PT', 'users/7/profile.webp', '2026-01-02 09:05:00', 'ELOXjF01xl3ZiUslNj30kUZdreo8BYaEit34GOz/z64=', 'ckGB7Z5+GO9rxxuSycfE7Q==', 'RESIDENCE_PERMIT', 'RP-778899');

INSERT INTO teacher_profile (id_user, cod_teacher) VALUES (6, 'TCH-002');
INSERT INTO student_profile (id_user, cod_student) VALUES (7, 'STD-007');

-- Additional structure: new class group and block
INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (51, 40, 30, 300, 3001, 'PRJ-PL2', 'online', 'active', 5, 35, '2026-01-01', '2026-06-30', 'morning');

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    state
) VALUES
    (61, 51, 'BLK-02', 'Planning', 'Second course unit block', 1, 'active');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (6, 51, 'active', '2026-02-01', NULL);

INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date) VALUES
    (7, 30, 300, 'active', '2026-01-01', '2026-06-30');

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (7, 51, 'active', '2026-02-01', NULL);

-- Additional content and lesson
INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at, updated_at
) VALUES
    (73, 6, 'Support Video', 'Iteration planning video', 'video', 'contents/videos/planning.mp4', 'active',
     '2026-02-15 09:00:00', NULL);

INSERT INTO associate_class_group_content (id_class_group, id_content_item, role) VALUES
    (51, 73, 'support');

INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory) VALUES
    (61, 73, 1, 'main', 0);

INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type, provider,
    access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (81, 51, 61, NULL, 'Online Lesson 1', 'Remote planning lesson', 'online', 'Teams',
     'https://teams.microsoft.com/l/meetup-join/prj-pl2', 1, 'active', '2026-02-18 09:00:00', '2026-02-18 11:00:00');

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
    max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode, state, available_from, available_until
) VALUES
    (93, 40, 61, NULL, 'PRJ-PL2 Online Form', 'Demo online class group assessment', 'form', 'online', 'automatic',
     20.00, 9.50, 100.00, 2, 'auto_approve', 'active', '2026-02-17 00:00:00', '2026-02-20 23:59:59');

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer
) VALUES
    (102, 93, 'Q1', 'What is the goal of sprint planning?', 'single_choice', 1, 1, 20.00, NULL);

INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag
) VALUES
    (114, 102, 1, 'Define and plan the sprint work', 1),
    (115, 102, 2, 'Close the current sprint', 0);

INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (7, 93, 'active');

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (142, 81, NULL, 'Evento Online Lesson 1', 'PRJ-PL2 online class group session', 'lesson',
     '2026-02-18 09:00:00', '2026-02-18 11:00:00', 0, 1, 20, 'active');

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (2, 142),
    (6, 142),
    (7, 142);

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (142, 51);

INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (151, 81, 7, 'late', 'automatic', '2026-02-18 09:12:00', '2026-02-18 11:00:00', 'Entry after start', 'active');

-- Second attempt and grade record
INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (121, 7, 93, 1, 8.00, 'corrected', '2026-02-18 10:00:00', '2026-02-18 10:12:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (131, 121, 102, 'R1', NULL, NULL, 8.00, '2026-02-18 10:06:00');

INSERT INTO response_option (id_response, id_option) VALUES
    (131, 114);

INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, state, recorded_at, notes
) VALUES
    (181, 170, 7, 121, 'GR-002', 8.00, 'failed', 'inactive', '2026-02-12 12:05:00',
     'Inactive seed record; active final grades are calculated automatically only after every weighted assessment has a corrected score.');

-- Demo messages in the channel
INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (210, 'PRJ-PL2 Online Channel', 'class_group', 'participants', '2026-02-15 09:55:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (6, 210, 'teacher', '2026-02-15 10:00:00', 0, 'active'),
    (7, 210, 'student', '2026-02-15 10:00:00', 0, 'active');

INSERT INTO associate_channel_class_group (id_channel, id_class_group) VALUES
    (210, 51);

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (224, 210, 6, NULL, 142, 'Online Lesson Preparation', 'Review the material before the session', 'announcement',
     'high', NULL, '2026-02-17 18:00:00', NULL, NULL, '2026-02-17 18:01:00', 'sent');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, state
) VALUES
    (7, 224, '2026-02-17 18:01:10', '2026-02-17 18:30:00', 'read');

-- Additional certificate
INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, validation_code, issued_at, state, final_grade
) VALUES
    (193, 30, 300, 7, 'Participation Certificate', 'Participation in the module', 'attendance', 'template-v1', NULL, NULL, 'draft', NULL);

-- Additional log
INSERT INTO activity_log (
    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
    affected_entity_identifier, occurred_at, outcome, source_ip
) VALUES
    (234, 6, NULL, 'SEND', 'message', '224', '2026-02-17 18:01:00', 'success', '127.0.0.1'),
    (235, 7, NULL, 'READ', 'message', '224', '2026-02-17 18:30:00', 'success', '127.0.0.1');

-- Additional full-mode coverage:
-- profiles, authorization contexts, process states, content, lessons,
-- assessments, attendance, grade sheets, certificates, channels, messages and audit records.

INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (8, 'Organization Scoped Admin', 'admin.org@gape.local', 'active', 'pt-PT', 'users/8/profile.webp',
     '2026-01-03 09:00:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-ORG-008'),
    (9, 'Organic Unit Scoped Admin', 'admin.unit@gape.local', 'active', 'pt-PT', 'users/9/profile.webp',
     '2026-01-03 09:05:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-UNIT-009'),
    (10, 'Structure Demo Admin', 'admin.structure.demo@gape.local', 'active', 'pt-PT', 'users/10/profile.webp',
     '2026-01-03 09:10:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'PASSPORT', 'PASS-STR-010'),
    (11, 'Unit Structure Demo Admin', 'admin.structure.unit@gape.local', 'active', 'pt-PT', 'users/11/profile.webp',
     '2026-01-03 09:15:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'PASSPORT', 'PASS-UNI-011'),
    (12, 'Multi Profile User', 'multi@gape.local', 'active', 'pt-PT', 'users/12/profile.webp',
     '2026-01-03 09:20:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-MULTI-012'),
    (13, 'Blocked Demo User', 'blocked.full@gape.local', 'blocked', 'pt-PT', 'users/13/profile.webp',
     '2026-01-03 09:25:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-BLOCK-013'),
    (14, 'Coordinator Two', 'coord2@gape.local', 'active', 'en-US', 'users/14/profile.webp',
     '2026-01-03 09:30:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', NULL, NULL),
    (15, 'Student Three', 'student3@gape.local', 'active', 'pt-PT', 'users/15/profile.webp',
     '2026-01-03 09:35:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'RESIDENCE_PERMIT', 'RP-STD-015'),
    (16, 'Company Admin', 'admin.company@gape.local', 'active', 'pt-PT', 'users/16/profile.webp',
     '2026-01-03 09:40:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-COMP-016');

INSERT INTO administrator_profile (id_user, cod_administrator) VALUES
    (8, 'ADM-ORG'),
    (9, 'ADM-UNIT'),
    (10, 'ADM-STR-DEMO'),
    (11, 'ADM-UNIT-DEMO'),
    (16, 'ADM-COMP');

INSERT INTO coordinator_profile (id_user, cod_coordinator) VALUES
    (12, 'COO-MULTI'),
    (14, 'COO-002');

INSERT INTO teacher_profile (id_user, cod_teacher) VALUES
    (12, 'TCH-MULTI');

INSERT INTO student_profile (id_user, cod_student) VALUES
    (12, 'STD-MULTI'),
    (15, 'STD-015');

INSERT INTO user_session (
    id_session, id_user, token, state, start_at, last_activity, end_at
) VALUES
    (101, 8, 'tok-admin-org-expired-101', 'expired', '2026-01-11 09:00:00', '2026-01-11 09:15:00', '2026-01-11 09:20:00'),
    (102, 4, 'tok-student-closed-102', 'closed', '2026-01-11 10:00:00', '2026-01-11 10:40:00', '2026-01-11 10:45:00'),
    (103, 12, 'tok-multi-active-103', 'active', '2026-01-11 11:00:00', '2026-01-11 11:20:00', NULL),
    (104, 15, 'tok-student3-active-104', 'active', '2026-01-11 12:00:00', '2026-01-11 12:05:00', NULL);

INSERT INTO permission (cod_permission, name, state) VALUES
    ('VIEW_REPORTS', 'View Reports', 'active'),
    ('LEGACY_IMPORT', 'Legacy Import', 'inactive');

INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id) VALUES
    (8, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIZATION', 10),
    (9, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIC_UNIT', 20),
    (10, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIZATION', 10),
    (11, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIC_UNIT', 20),
    (16, 'MANAGE_ORGANIZATION_STRUCTURE', 'ORGANIZATION', 12);

INSERT INTO grant_coordinator (id_coordinator_user, cod_permission) VALUES
    (2, 'VIEW_REPORTS'),
    (12, 'VIEW_REPORTS'),
    (12, 'MANAGE_LEARNING'),
    (14, 'VIEW_REPORTS'),
    (14, 'MANAGE_LEARNING');

INSERT INTO grant_teacher (id_teacher_user, cod_permission) VALUES
    (3, 'VIEW_REPORTS'),
    (6, 'VIEW_REPORTS'),
    (6, 'MANAGE_LEARNING'),
    (12, 'VIEW_REPORTS'),
    (12, 'MANAGE_LEARNING');

INSERT INTO grant_student (id_student_user, cod_permission) VALUES
    (4, 'VIEW_REPORTS'),
    (7, 'VIEW_REPORTS'),
    (12, 'VIEW_REPORTS'),
    (15, 'VIEW_REPORTS');

INSERT INTO deletion_request (
    id_deletion, submitter_user_id, processor_admin_user_id, submitted_at, processed_at, reason, state
) VALUES
    (300, 4, NULL, '2026-01-12 09:00:00', NULL, 'Request under initial review', 'submitted'),
    (301, 7, NULL, '2026-01-12 09:10:00', NULL, 'Request forwarded for verification', 'under_review'),
    (302, 5, 1, '2026-01-12 09:20:00', '2026-01-13 10:00:00', 'Request approved for inactive account', 'approved'),
    (303, 13, 8, '2026-01-12 09:30:00', '2026-01-13 10:10:00', 'Blocked account with active dependencies', 'rejected'),
    (304, 15, 1, '2026-01-12 09:40:00', '2026-01-13 10:20:00', 'Demo anonymization completed', 'completed');

INSERT INTO organization (id_organization, name, acronym, photo, type, state) VALUES
    (12, 'GAPE Business Academy', 'AEG', 'organizations/12/profile.webp', 'company', 'inactive'),
    (13, 'GAPE Archive', 'ARG', 'organizations/13/profile.webp', 'other', 'inactive');

INSERT INTO manage_organization (id_admin_user, id_organization, state, start_date, end_date) VALUES
    (8, 10, 'active', '2026-01-01', NULL),
    (16, 12, 'active', '2026-01-01', NULL),
    (1, 13, 'active', '2026-01-01', NULL);

UPDATE organization
SET state = 'active'
WHERE id_organization IN (12, 13);

INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (23, 10, 'QA', 'Academic Quality Office', 'GQA', 'office', 'active', 20),
    (24, 12, 'FOR', 'Training Department', 'DFOR', 'direction', 'active', NULL),
    (25, 10, 'LEG', 'Historical Unit', 'UH', 'service', 'inactive', NULL),
    (26, 13, 'ARC', 'Archive Unit', 'ARC', 'service', 'active', NULL);

INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, photo, description, ects, duration, frequency, type, state
) VALUES
    (32, 10, 22, 'Information Systems Master', 'MSI', NULL, 'Second-cycle course for full coverage', 120.00, '1', 'semester', 'master', 'active'),
    (33, 12, 24, 'Systems Quality and Audit', 'QAS', NULL, 'Professional quality training', 20.00, '1', 'quadrimester', 'professional_training', 'active'),
    (34, 10, 22, 'Mathematics 1', 'MATH1', NULL, 'Three-year mathematics degree used to demonstrate course occurrences.', 180.00, '3', 'semester', 'degree', 'active'),
    (35, 13, 26, 'Archived Programme', 'ARCP', NULL, 'Historical programme retained for archived academic content.', 3.00, '1', 'annual', 'other', 'active'),
    (3008, 10, 22, 'Mathematics 2', 'MATH2', NULL, 'Three-year mathematics degree sharing the current academic calendar with Mathematics 1.', 180.00, '3', 'semester', 'degree', 'active');

INSERT INTO course_period_template (
    id_course, curricular_year, term, starts_month, starts_day, ends_month, ends_day
) VALUES
    (32, 1, 'semester_1', 1, 1, 6, 30),
    (32, 1, 'semester_2', 7, 1, 12, 31),
    (33, 1, 'quadrimester_1', 1, 1, 4, 30),
    (33, 1, 'quadrimester_2', 5, 1, 8, 31),
    (33, 1, 'quadrimester_3', 9, 1, 12, 31),
    (34, 1, 'semester_1', 9, 1, 2, 1),
    (34, 1, 'semester_2', 3, 1, 8, 1),
    (34, 2, 'semester_1', 9, 1, 2, 1),
    (34, 2, 'semester_2', 3, 1, 8, 1),
    (34, 3, 'semester_1', 9, 1, 2, 1),
    (34, 3, 'semester_2', 3, 1, 8, 1),
    (35, 1, 'annual', 1, 1, 12, 31),
    (3008, 1, 'semester_1', 9, 1, 2, 1),
    (3008, 1, 'semester_2', 3, 1, 8, 1),
    (3008, 2, 'semester_1', 9, 1, 2, 1),
    (3008, 2, 'semester_2', 3, 1, 8, 1),
    (3008, 3, 'semester_1', 9, 1, 2, 1),
    (3008, 3, 'semester_2', 3, 1, 8, 1);

INSERT INTO course_occurrence (
    id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
) VALUES
    (320, 32, 2025, '2025', '2025-01-01', '2025-12-31', 'completed'),
    (321, 32, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (330, 33, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (340, 34, 2020, '2020-2021', '2020-09-01', '2021-08-01', 'completed'),
    (341, 34, 2021, '2021-2022', '2021-09-01', '2022-08-01', 'completed'),
    (342, 34, 2022, '2022-2023', '2022-09-01', '2023-08-01', 'completed'),
    (343, 34, 2023, '2023-2024', '2023-09-01', '2024-08-01', 'completed'),
    (344, 34, 2024, '2024-2025', '2024-09-01', '2025-08-01', 'completed'),
    (345, 34, 2025, '2025-2026', '2025-09-01', '2026-08-01', 'active'),
    (346, 34, 2026, '2026-2027', '2026-09-01', '2027-08-01', 'scheduled'),
    (347, 34, 2027, '2027-2028', '2027-09-01', '2028-08-01', 'scheduled'),
    (348, 34, 2028, '2028-2029', '2028-09-01', '2029-08-01', 'scheduled'),
    (349, 34, 2029, '2029-2030', '2029-09-01', '2030-08-01', 'scheduled'),
    (30071, 3008, 2025, '2025-2026', '2025-09-01', '2026-08-01', 'active');

INSERT INTO course_occurrence_period (
    id_course_occurrence_period, id_course_occurrence, curricular_year, term, starts_at, ends_at, state
) VALUES
    (3201, 321, 1, 'semester_1', '2026-01-01', '2026-06-30', 'active'),
    (3202, 321, 1, 'semester_2', '2026-07-01', '2026-12-31', 'active'),
    (3211, 320, 1, 'semester_1', '2025-01-01', '2025-06-30', 'completed'),
    (3212, 320, 1, 'semester_2', '2025-07-01', '2025-12-31', 'completed'),
    (3301, 330, 1, 'quadrimester_1', '2026-01-01', '2026-04-30', 'active'),
    (3302, 330, 1, 'quadrimester_2', '2026-05-01', '2026-08-31', 'active'),
    (3303, 330, 1, 'quadrimester_3', '2026-09-01', '2026-12-31', 'active');

INSERT INTO course_occurrence_period (
    id_course_occurrence_period, id_course_occurrence, curricular_year, term, starts_at, ends_at, state
) VALUES
    (34001, 340, 1, 'semester_1', '2020-09-01', '2021-02-01', 'completed'),
    (34002, 340, 1, 'semester_2', '2021-03-01', '2021-08-01', 'completed'),
    (34003, 340, 2, 'semester_1', '2020-09-01', '2021-02-01', 'completed'),
    (34004, 340, 2, 'semester_2', '2021-03-01', '2021-08-01', 'completed'),
    (34005, 340, 3, 'semester_1', '2020-09-01', '2021-02-01', 'completed'),
    (34006, 340, 3, 'semester_2', '2021-03-01', '2021-08-01', 'completed'),
    (34101, 341, 1, 'semester_1', '2021-09-01', '2022-02-01', 'completed'),
    (34102, 341, 1, 'semester_2', '2022-03-01', '2022-08-01', 'completed'),
    (34103, 341, 2, 'semester_1', '2021-09-01', '2022-02-01', 'completed'),
    (34104, 341, 2, 'semester_2', '2022-03-01', '2022-08-01', 'completed'),
    (34105, 341, 3, 'semester_1', '2021-09-01', '2022-02-01', 'completed'),
    (34106, 341, 3, 'semester_2', '2022-03-01', '2022-08-01', 'completed'),
    (34201, 342, 1, 'semester_1', '2022-09-01', '2023-02-01', 'completed'),
    (34202, 342, 1, 'semester_2', '2023-03-01', '2023-08-01', 'completed'),
    (34203, 342, 2, 'semester_1', '2022-09-01', '2023-02-01', 'completed'),
    (34204, 342, 2, 'semester_2', '2023-03-01', '2023-08-01', 'completed'),
    (34205, 342, 3, 'semester_1', '2022-09-01', '2023-02-01', 'completed'),
    (34206, 342, 3, 'semester_2', '2023-03-01', '2023-08-01', 'completed'),
    (34301, 343, 1, 'semester_1', '2023-09-01', '2024-02-01', 'completed'),
    (34302, 343, 1, 'semester_2', '2024-03-01', '2024-08-01', 'completed'),
    (34303, 343, 2, 'semester_1', '2023-09-01', '2024-02-01', 'completed'),
    (34304, 343, 2, 'semester_2', '2024-03-01', '2024-08-01', 'completed'),
    (34305, 343, 3, 'semester_1', '2023-09-01', '2024-02-01', 'completed'),
    (34306, 343, 3, 'semester_2', '2024-03-01', '2024-08-01', 'completed'),
    (34401, 344, 1, 'semester_1', '2024-09-01', '2025-02-01', 'completed'),
    (34402, 344, 1, 'semester_2', '2025-03-01', '2025-08-01', 'completed'),
    (34403, 344, 2, 'semester_1', '2024-09-01', '2025-02-01', 'completed'),
    (34404, 344, 2, 'semester_2', '2025-03-01', '2025-08-01', 'completed'),
    (34405, 344, 3, 'semester_1', '2024-09-01', '2025-02-01', 'completed'),
    (34406, 344, 3, 'semester_2', '2025-03-01', '2025-08-01', 'completed'),
    (34501, 345, 1, 'semester_1', '2025-09-01', '2026-02-01', 'completed'),
    (34502, 345, 1, 'semester_2', '2026-03-01', '2026-08-01', 'active'),
    (34503, 345, 2, 'semester_1', '2025-09-01', '2026-02-01', 'completed'),
    (34504, 345, 2, 'semester_2', '2026-03-01', '2026-08-01', 'active'),
    (34505, 345, 3, 'semester_1', '2025-09-01', '2026-02-01', 'completed'),
    (34506, 345, 3, 'semester_2', '2026-03-01', '2026-08-01', 'active'),
    (34601, 346, 1, 'semester_1', '2026-09-01', '2027-02-01', 'scheduled'),
    (34602, 346, 1, 'semester_2', '2027-03-01', '2027-08-01', 'scheduled'),
    (34603, 346, 2, 'semester_1', '2026-09-01', '2027-02-01', 'scheduled'),
    (34604, 346, 2, 'semester_2', '2027-03-01', '2027-08-01', 'scheduled'),
    (34605, 346, 3, 'semester_1', '2026-09-01', '2027-02-01', 'scheduled'),
    (34606, 346, 3, 'semester_2', '2027-03-01', '2027-08-01', 'scheduled'),
    (34701, 347, 1, 'semester_1', '2027-09-01', '2028-02-01', 'scheduled'),
    (34702, 347, 1, 'semester_2', '2028-03-01', '2028-08-01', 'scheduled'),
    (34703, 347, 2, 'semester_1', '2027-09-01', '2028-02-01', 'scheduled'),
    (34704, 347, 2, 'semester_2', '2028-03-01', '2028-08-01', 'scheduled'),
    (34705, 347, 3, 'semester_1', '2027-09-01', '2028-02-01', 'scheduled'),
    (34706, 347, 3, 'semester_2', '2028-03-01', '2028-08-01', 'scheduled'),
    (34801, 348, 1, 'semester_1', '2028-09-01', '2029-02-01', 'scheduled'),
    (34802, 348, 1, 'semester_2', '2029-03-01', '2029-08-01', 'scheduled'),
    (34803, 348, 2, 'semester_1', '2028-09-01', '2029-02-01', 'scheduled'),
    (34804, 348, 2, 'semester_2', '2029-03-01', '2029-08-01', 'scheduled'),
    (34805, 348, 3, 'semester_1', '2028-09-01', '2029-02-01', 'scheduled'),
    (34806, 348, 3, 'semester_2', '2029-03-01', '2029-08-01', 'scheduled'),
    (34901, 349, 1, 'semester_1', '2029-09-01', '2030-02-01', 'scheduled'),
    (34902, 349, 1, 'semester_2', '2030-03-01', '2030-08-01', 'scheduled'),
    (34903, 349, 2, 'semester_1', '2029-09-01', '2030-02-01', 'scheduled'),
    (34904, 349, 2, 'semester_2', '2030-03-01', '2030-08-01', 'scheduled'),
    (34905, 349, 3, 'semester_1', '2029-09-01', '2030-02-01', 'scheduled'),
    (34906, 349, 3, 'semester_2', '2030-03-01', '2030-08-01', 'scheduled'),
    (300075, 30071, 1, 'semester_2', '2026-03-01', '2026-08-01', 'active'),
    (300076, 30071, 1, 'semester_1', '2025-09-01', '2026-02-01', 'completed'),
    (300077, 30071, 2, 'semester_1', '2025-09-01', '2026-02-01', 'completed'),
    (300078, 30071, 2, 'semester_2', '2026-03-01', '2026-08-01', 'active'),
    (300079, 30071, 3, 'semester_1', '2025-09-01', '2026-02-01', 'completed'),
    (300080, 30071, 3, 'semester_2', '2026-03-01', '2026-08-01', 'active');

INSERT INTO subject (
    id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
) VALUES
    (42, 10, 'Databases', 'BD', NULL, 'Relational modeling and persistence', 6.00, 70, 'active'),
    (43, 10, 'Computer Networks', 'RC', NULL, 'Network and service fundamentals', 6.00, 65, 'active'),
    (44, 12, 'Industrial Safety', 'SI', NULL, 'Safety subject in a business context', 4.00, 30, 'active'),
    (45, 13, 'Inactive Content', 'ARQ', NULL, 'Historical subject for inactive organization', 3.00, 20, 'inactive'),
    (46, 10, 'Ethics and Digital Society', 'EDS', NULL, 'Approved subject awaiting curricular placement.', 3.00, 30, 'active');

INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory
) VALUES
    (30, 42, 1, 'semester_1', 1),
    (32, 42, 1, 'semester_1', 1),
    (32, 43, 1, 'semester_1', 0),
    (34, 43, 1, 'semester_2', 0),
    (3008, 43, 1, 'semester_2', 0),
    (33, 44, 1, 'quadrimester_1', 1);

-- This inactive subject's previous curricular placement is a complete
-- historical record.  Other inactive subjects may retain active associations
-- when those associations existed before the subject was deactivated.
INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory, state, ended_at
) VALUES
    (35, 45, 1, 'annual', 1, 'historical', '2026-06-30');

INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state) VALUES
    (14, 42, 'active'),
    (12, 43, 'active'),
    (2, 41, 'inactive');

INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (53, 42, 30, 300, 3001, 'BD-T1', 'hybrid', 'active', 8, 35, '2026-01-01', '2026-06-30', 'evening'),
    (54, 43, 32, 321, 3201, 'RC-MSI-01', 'onsite', 'completed', 6, 25, '2026-01-01', '2026-06-30', 'morning'),
    (55, 44, 33, 330, 3301, 'SI-T1', 'online', 'active', 4, 20, '2026-01-01', '2026-04-30', 'afternoon'),
    (56, 42, 32, 320, 3211, 'BD-ARCH', 'online', 'completed', 5, 20, '2025-01-01', '2025-06-30', 'mixed'),
    (57, 43, 32, 321, 3201, 'RC-MSI-02', 'onsite', 'completed', 5, 25, '2026-01-01', '2026-06-30', 'morning'),
    (58, 43, 32, 321, 3201, 'RC-MSI-03', 'online', 'completed', 5, 25, '2026-01-01', '2026-06-30', 'afternoon'),
    (59, 43, 32, 321, 3201, 'RC-MSI-04', 'hybrid', 'completed', 5, 25, '2026-01-01', '2026-06-30', 'evening'),
    (60, 43, 32, 321, 3201, 'RC-MSI-05', 'onsite', 'completed', 5, 25, '2026-01-01', '2026-06-30', 'mixed'),
    (61, 43, 34, 345, 34502, 'RC-MATH-01', 'online', 'active', 5, 25, '2026-03-01', '2026-08-01', 'morning'),
    (62, 43, 34, 345, 34502, 'RC-MATH-02', 'hybrid', 'active', 5, 25, '2026-03-01', '2026-08-01', 'afternoon'),
    (63, 43, 34, 345, 34502, 'RC-MATH-03', 'onsite', 'active', 5, 25, '2026-03-01', '2026-08-01', 'evening'),
    (64, 43, 34, 345, 34502, 'RC-MATH-04', 'online', 'active', 5, 25, '2026-03-01', '2026-08-01', 'mixed'),
    (65, 43, 34, 345, 34502, 'RC-MATH-05', 'hybrid', 'active', 5, 25, '2026-03-01', '2026-08-01', 'morning'),
    (3007, 43, 3008, 30071, 300075, 'uy', 'onsite', 'active', 1, 20, '2026-03-01', '2026-08-01', 'morning');

-- Computer Networks is intentionally active in two independent course
-- calendars. Information Systems Master runs calendar semesters (Jan-Jun /
-- Jul-Dec), while Mathematics 1 and Mathematics 2 use academic-year semesters
-- (Sep-Feb / Mar-Aug). Each class group remains attached to exactly one of
-- those real course-occurrence periods; they are never mirrored across courses.

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    state
) VALUES
    (63, 53, 'BD-01', 'Relational Model', 'Block about data modeling', 1, 'active'),
    (64, 53, 'BD-02', 'Applied SQL', 'Block in preparation', 2, 'active'),
    (65, 55, 'SI-01', 'Safety Standards', 'Initial business training block', 1, 'active'),
    (66, 54, 'RC-LEG', 'Closed Laboratory', 'Historical networks block', 1, 'inactive');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (3, 53, 'active', '2026-03-01', NULL),
    (6, 53, 'inactive', '2026-03-01', '2026-03-31'),
    (12, 55, 'active', '2026-04-01', NULL);

INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date) VALUES
    (4, 32, 321, 'active', '2026-03-01', NULL),
    (7, 32, 321, 'active', '2026-03-01', NULL),
    (12, 32, 321, 'active', '2026-03-01', NULL),
    (12, 33, 330, 'active', '2026-01-01', '2026-04-30'),
    (15, 30, 300, 'active', '2026-01-01', '2026-06-30'),
    (15, 32, 321, 'active', '2026-03-01', NULL),
    (15, 33, 330, 'active', '2026-01-01', '2026-04-30');

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (4, 53, 'active', '2026-03-01', NULL),
    (12, 55, 'active', '2026-04-01', NULL),
    (15, 53, 'active', '2026-03-01', NULL),
    (15, 54, 'completed', '2026-03-01', '2026-05-31'),
    (15, 55, 'active', '2026-04-01', NULL);

INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at, updated_at
) VALUES
    (74, 3, 'Entity Relationship Diagram', 'Support image for the relational model', 'image', 'contents/images/er.webp', 'active',
     '2026-03-02 09:00:00', NULL),
    (75, 3, 'Normalization Video', 'Normalization demo video', 'video', 'contents/videos/normalization.mp4', 'active',
     '2026-03-02 09:10:00', NULL),
    (76, 14, 'Digital Library Link', 'External research resource', 'url', 'https://biblioteca.example.local', 'active',
     '2026-03-02 09:20:00', NULL),
    (77, 12, 'Safety Slides', 'Industrial safety presentation', 'presentation', 'contents/presentations/industrial-safety-slides.pdf', 'draft',
     '2026-04-02 09:00:00', NULL),
    (78, 12, 'Quality SCORM Package', 'Imported SCORM module', 'scorm', 'contents/packages/scorm-quality.zip', 'inactive',
     '2026-04-02 09:15:00', '2026-04-03 11:00:00'),
    (79, 12, 'Audit xAPI Package', 'xAPI experience for auditing', 'xapi', 'contents/packages/xapi-audit.zip', 'active',
     '2026-04-02 09:30:00', NULL),
    (80, 12, 'Embedded Simulator', 'Ferramenta externa incorporada', 'embed', 'https://tools.example.local/embed/safety', 'active',
     '2026-04-02 09:45:00', NULL),
    (81, 6, 'Review Audio', 'Audio lesson summary', 'audio', 'contents/audio/review.m4a', 'active',
     '2026-03-03 12:00:00', NULL),
    (82, 6, 'Historical Object', 'Item without a specific archival format', 'other', NULL, 'inactive',
     '2026-01-01 12:00:00', NULL);

INSERT INTO associate_organization_content (id_organization, id_content_item, role) VALUES
    (10, 76, 'reference'),
    (12, 80, 'tool');

INSERT INTO associate_organic_unit_content (id_organic_unit, id_content_item, role) VALUES
    (20, 74, 'support'),
    (24, 79, 'main');

INSERT INTO associate_course_content (id_course, id_content_item, role) VALUES
    (30, 75, 'main'),
    (33, 80, 'tool');

INSERT INTO associate_subject_content (id_subject, id_content_item, role) VALUES
    (42, 76, 'reference'),
    (44, 77, 'support');

INSERT INTO associate_class_group_content (id_class_group, id_content_item, role) VALUES
    (53, 74, 'support'),
    (55, 80, 'tool');

INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory) VALUES
    (63, 74, 1, 'main', 1),
    (63, 75, 2, 'support', 0),
    (64, 76, 1, 'reference', 0),
    (65, 77, 1, 'main', 1),
    (65, 80, 2, 'tool', 1);

INSERT INTO physical_room (
    cod_physical_room, id_organization, id_organic_unit, name, description, capacity, location, state
) VALUES
    ('SALA-B1', 10, 22, 'Room B1', 'Database room', 35, 'Building B', 'active'),
    ('SALA-HYB', 10, 20, 'Hybrid Room', 'Room equipped for hybrid classes', 30, 'Building A', 'active'),
    ('SALA-EX01', 10, 20, 'Exam Room 01', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX02', 10, 20, 'Exam Room 02', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX03', 10, 20, 'Exam Room 03', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX04', 10, 20, 'Exam Room 04', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX05', 10, 20, 'Exam Room 05', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX06', 10, 20, 'Exam Room 06', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX07', 10, 20, 'Exam Room 07', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX08', 10, 20, 'Exam Room 08', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX09', 10, 20, 'Exam Room 09', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX10', 10, 20, 'Exam Room 10', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX11', 10, 20, 'Exam Room 11', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX12', 10, 20, 'Exam Room 12', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX13', 10, 20, 'Exam Room 13', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX14', 10, 20, 'Exam Room 14', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-EX15', 10, 20, 'Exam Room 15', 'Room for in-person assessments', 60, 'Exam Building', 'active'),
    ('SALA-C1', 12, 24, 'Room C1', 'Business training room', 18, 'Business Campus', 'active'),
    ('ROOM-INACTIVE', 12, 24, 'Inactive room', 'Temporarily unavailable space', 10, 'Business Campus', 'inactive');

INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at
) VALUES
    (82, 53, 63, 'SALA-HYB', 'Databases Hybrid Lesson', 'Session about the relational model', 'hybrid',
     'Zoom', 'https://zoom.us/j/98765432101', 1, 'active', '2026-03-05 18:00:00', '2026-03-05 20:00:00'),
    (83, 53, 64, 'SALA-B1', 'SQL Laboratory', 'Scheduled in-person session', 'onsite',
     NULL, NULL, 1, 'scheduled', '2026-04-10 18:00:00', '2026-04-10 20:00:00'),
    (84, 55, 65, NULL, 'Online Safety Lesson', 'Online safety training', 'online',
     'Teams', 'https://teams.microsoft.com/l/meetup-join/si-t1', 1, 'active', '2026-04-08 15:00:00', '2026-04-08 17:00:00'),
    (85, 54, 66, 'SALA-B1', 'Closed Laboratory RC', 'Historical networks session', 'onsite',
     NULL, NULL, 0, 'completed', '2026-03-15 09:00:00', '2026-03-15 11:00:00');

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
    max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode, state, available_from, available_until
) VALUES
    (94, 42, 63, 'SALA-EX01', 'Databases Practical Exam', 'Practical databases assessment', 'exam', 'onsite', 'manual',
     20.00, 9.50, 80.00, 2, 'manual', 'active', '2026-03-10 09:00:00', '2026-03-10 11:00:00'),
    (95, 42, 64, NULL, 'SQL Draft Form', 'Form in preparation', 'form', 'online', 'automatic',
     20.00, 10.00, 20.00, 3, 'auto_approve', 'draft', '2026-04-01 00:00:00', '2026-04-15 23:59:59'),
    (96, 44, 65, NULL, 'Safety Form', 'Online industrial safety assessment', 'form', 'online', 'automatic',
     20.00, 10.00, 100.00, 1, 'auto_approve', 'active', '2026-04-09 00:00:00', '2026-04-12 23:59:59'),
    (97, 43, NULL, 'SALA-EX02', 'Networks Exam', 'Networks exam without pedagogical block', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'manual', 'completed', '2026-05-20 09:00:00', '2026-05-20 11:00:00'),
    (98, 42, 63, NULL, 'Complete Databases Assessment', 'Demo assessment with all question types', 'test', 'online', 'mixed',
     20.00, 10.00, 0.00, 3, 'auto_approve', 'active', '2026-03-18 08:00:00', '2026-03-28 23:59:59'),
    (199, 42, 63, NULL, 'Correct Attempt QA Matrix', 'Visual QA assessment covering expected-answer states in correction.', 'test', 'online', 'mixed',
     20.00, 10.00, 0.00, 1, 'auto_approve', 'active', '2026-03-22 08:00:00', '2026-03-29 23:59:59'),
    -- A realistic shared final for the completed Information Systems Master network groups.
    -- Some students have a corrected result while other valid submissions are
    -- still awaiting correction, which exercises the normal Draft/Published
    -- grade-sheet lifecycle without artificial zeroes.
    (4000, 43, NULL, 'SALA-EX02', 'Network Foundations Final',
     'Shared final assessment for the completed Computer Networks class groups in Information Systems Master.',
     'exam', 'onsite', 'manual', 20.00, 9.50, 100.00, 1, 'manual', 'completed',
     '2026-05-22 09:00:00', '2026-05-22 11:00:00'),
    -- The current groups have a genuine scheduled assessment rather than an
    -- empty artificial context.
    (4001, 43, NULL, 'SALA-EX02', 'Network Services Review',
     'Scheduled continuous review for the active Computer Networks class groups in Mathematics 1.',
     'exam', 'onsite', 'manual', 20.00, 9.50, 100.00, 2, 'auto_approve', 'scheduled',
     '2026-07-22 09:00:00', '2026-07-22 11:00:00');

INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (97, 54),
    (4000, 57),
    (4000, 58),
    (4000, 59),
    (4000, 60),
    (4001, 61),
    (4001, 62),
    (4001, 63),
    (4001, 64);

INSERT INTO associate_assessment_content (id_assessment, id_content_item, role) VALUES
    (94, 74, 'statement'),
    (95, 76, 'support'),
    (96, 77, 'statement');

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer
) VALUES
    (103, 94, 'BD-Q1', 'Select the applicable normal forms.', 'multiple_choice', 1, 1, 8.00, NULL),
    (104, 94, 'BD-Q2', 'Explain the difference between a primary key and a foreign key.', 'paragraph', 2, 1, 8.00,
     'Free response assessed manually'),
    (105, 94, 'BD-Q3', 'Attach the final SQL script.', 'file_upload', 3, 1, 4.00, NULL),
    (106, 95, 'SQL-Q1', 'Choose the query command.', 'single_choice', 1, 1, 20.00, NULL),
    (107, 96, 'SI-Q1', 'Rate the displayed risk.', 'rating', 1, 1, 20.00, '4'),
    (109, 97, 'RC-Q1', 'How many usable hosts does a /24 mask provide?', 'short_text', 1, 1, 20.00, '254'),
    (110, 98, 'FULL-Q1', 'Choose the SQL statement used to query data.', 'single_choice', 1, 1, 2.00, NULL),
    (111, 98, 'FULL-Q2', 'Select all properties that belong to an ACID transaction.', 'multiple_choice', 2, 1, 3.00, NULL),
    (112, 98, 'FULL-Q3', 'Enter the keyword used to remove rows from a table.', 'short_text', 3, 1, 2.00, 'DELETE'),
    (113, 98, 'FULL-Q4', 'Explain when an index should be created on a column.', 'paragraph', 4, 1, 4.00,
     'Free response automatically assessed as textual reference.'),
    (114, 98, 'FULL-Q5', 'Attach the final entity-relationship diagram.', 'file_upload', 5, 1, 3.00, 'formats=pdf,image,archive'),
    (116, 98, 'FULL-Q6', 'Rate the clarity of the prompt.', 'rating', 6, 1, 6.00,
     'rating_style=stars;rating_step=half;rating_max=5;expected_value=4.5'),
    (301, 199, 'QA-SC-C', 'Single choice with expected answer and a correct student answer.', 'single_choice', 1, 1, 2.00, NULL),
    (302, 199, 'QA-SC-W', 'Single choice with expected answer and a wrong student answer.', 'single_choice', 2, 1, 2.00, NULL),
    (303, 199, 'QA-SC-N', 'Single choice without expected answer must stay neutral.', 'single_choice', 3, 0, 1.00, NULL),
    (304, 199, 'QA-MC-MIX', 'Multiple choice with one correct and one wrong selected option.', 'multiple_choice', 4, 1, 3.00, NULL),
    (305, 199, 'QA-MC-N', 'Multiple choice without expected answers must stay neutral.', 'multiple_choice', 5, 0, 1.00, NULL),
    (306, 199, 'QA-RT-C', 'Rating with expected value and a matching answer.', 'rating', 6, 1, 2.00,
     'rating_style=stars;rating_step=half;rating_max=5;expected_value=4'),
    (307, 199, 'QA-RT-W', 'Rating with expected value and a different answer.', 'rating', 7, 1, 2.00,
     'rating_style=stars;rating_step=half;rating_max=5;expected_value=5'),
    (308, 199, 'QA-ST-E', 'Short text with an expected answer must show neutral comparison.', 'short_text', 8, 1, 2.00, 'DELETE'),
    (309, 199, 'QA-PA-E', 'Paragraph with an expected answer must show it separated below.', 'paragraph', 9, 1, 3.00,
     'Mention the indexed column is frequently used in filters, joins or ordering.'),
    (310, 199, 'QA-UP-E', 'Upload question should show accepted formats as expected information.', 'file_upload', 10, 1, 1.00,
     'formats=pdf,image'),
    (311, 199, 'QA-ST-N', 'Short text without expected answer must not show green or expected area.', 'short_text', 11, 0, 1.00, NULL),
    (4000, 4000, 'RC-FIN-01',
     'Which protocol family defines the addressing and routing foundation of the Internet?',
     'short_text', 1, 1, 20.00, 'IP'),
    (4001, 4001, 'RC-REV-01',
     'Name the transport protocol normally used for reliable web traffic.',
     'short_text', 1, 1, 20.00, 'TCP');

INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag
) VALUES
    (116, 103, 1, 'First normal form', 1),
    (117, 103, 2, 'Second normal form', 1),
    (118, 103, 3, 'Non-normalized form', 0),
    (119, 106, 1, 'SELECT', 1),
    (120, 106, 2, 'DROP', 0),
    (121, 107, 1, '1', 0),
    (122, 107, 2, '4', 1),
    (123, 107, 3, '5', 0),
    (124, 110, 1, 'SELECT', 1),
    (125, 110, 2, 'UPDATE', 0),
    (126, 110, 3, 'CREATE INDEX', 0),
    (127, 111, 1, 'Atomicity', 1),
    (128, 111, 2, 'Consistency', 1),
    (129, 111, 3, 'Durability', 1),
    (130, 111, 4, 'Rendering', 0),
    (401, 301, 1, 'SELECT', 1),
    (402, 301, 2, 'UPDATE', 0),
    (403, 302, 1, 'Primary key', 1),
    (404, 302, 2, 'Temporary cache', 0),
    (405, 303, 1, 'Manual review option A', NULL),
    (406, 303, 2, 'Manual review option B', NULL),
    (407, 304, 1, 'Atomicity', 1),
    (408, 304, 2, 'Consistency', 1),
    (409, 304, 3, 'Screen rendering', 0),
    (410, 304, 4, 'Durability', 1),
    (411, 305, 1, 'Neutral selected option', NULL),
    (412, 305, 2, 'Neutral unselected option', NULL),
    (413, 305, 3, 'Second neutral selected option', NULL);

INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (4, 94, 'pending'),
    (15, 94, 'active'),
    (15, 95, 'active'),
    (12, 96, 'active'),
    (15, 97, 'active'),
    (4, 98, 'active'),
    (15, 98, 'active'),
    (15, 199, 'active');

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (122, 15, 94, 1, 16.00, 'corrected', '2026-03-10 09:05:00', '2026-03-10 10:40:00'),
    (123, 15, 95, 1, NULL, 'in_progress', '2026-04-03 10:00:00', NULL),
    (124, 12, 96, 1, 15.00, 'corrected', '2026-04-09 14:00:00', '2026-04-09 14:25:00'),
    (125, 15, 94, 2, NULL, 'expired', '2026-03-10 10:45:00', NULL),
    (126, 15, 97, 1, 12.00, 'corrected', '2026-05-20 09:00:00', '2026-05-20 10:15:00'),
    (127, 15, 98, 1, NULL, 'submitted', '2026-03-18 09:00:00', '2026-03-18 09:55:00'),
    (128, 15, 98, 2, NULL, 'in_progress', '2026-03-19 10:00:00', NULL),
    (129, 4, 98, 1, NULL, 'cancelled', '2026-03-18 11:00:00', NULL),
    (501, 15, 199, 1, NULL, 'submitted', '2026-03-22 09:00:00', '2026-03-22 10:10:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (132, 122, 103, 'BD-R1', NULL, NULL, 8.00, '2026-03-10 09:30:00'),
    (133, 122, 104, 'BD-R2', 'The primary key identifies the record and the foreign key references another record.', NULL, 6.00,
     '2026-03-10 10:00:00'),
    (134, 122, 105, 'BD-R3', NULL, '/submissions/122/script.sql', 2.00, '2026-03-10 10:35:00'),
    (135, 123, 106, 'SQL-R1', NULL, NULL, NULL, '2026-04-03 10:10:00'),
    (136, 124, 107, 'SI-R1', NULL, NULL, 15.00, '2026-04-09 14:10:00'),
    (138, 126, 109, 'RC-R1', '254', NULL, 12.00, '2026-05-20 09:30:00'),
    (139, 127, 110, 'FULL-R1', NULL, NULL, 2.00, '2026-03-18 09:06:00'),
    (140, 127, 111, 'FULL-R2', NULL, NULL, 2.50, '2026-03-18 09:12:00'),
    (141, 127, 112, 'FULL-R3', 'DELETE', NULL, 2.00, '2026-03-18 09:18:00'),
    (142, 127, 113, 'FULL-R4', 'An index should be created when the column is frequently used in filters, joins, or ordering.', NULL,
     3.00, '2026-03-18 09:30:00'),
    (143, 127, 114, 'FULL-R5', NULL, '/submissions/127/diagrama-er.pdf', 2.00, '2026-03-18 09:40:00'),
    (145, 127, 116, 'FULL-R6', '4.5', NULL, 3.00, '2026-03-18 09:50:00'),
    (601, 501, 301, 'QA-R1', NULL, NULL, 2.00, '2026-03-22 09:05:00'),
    (602, 501, 302, 'QA-R2', NULL, NULL, 0.00, '2026-03-22 09:10:00'),
    (603, 501, 303, 'QA-R3', NULL, NULL, NULL, '2026-03-22 09:15:00'),
    (604, 501, 304, 'QA-R4', NULL, NULL, 0.00, '2026-03-22 09:20:00'),
    (605, 501, 305, 'QA-R5', NULL, NULL, NULL, '2026-03-22 09:25:00'),
    (606, 501, 306, 'QA-R6', '4', NULL, 2.00, '2026-03-22 09:30:00'),
    (607, 501, 307, 'QA-R7', '3.5', NULL, 0.00, '2026-03-22 09:35:00'),
    (608, 501, 308, 'QA-R8', 'DROP', NULL, NULL, '2026-03-22 09:40:00'),
    (609, 501, 309, 'QA-R9', 'Indexes help when a column is used repeatedly in filters and joins.', NULL, NULL,
     '2026-03-22 09:50:00'),
    (610, 501, 310, 'QA-R10', NULL, '/submissions/501/correction-matrix.png', NULL, '2026-03-22 10:00:00'),
    (611, 501, 311, 'QA-R11', 'Neutral free text answer.', NULL, NULL, '2026-03-22 10:05:00');

INSERT INTO response_option (id_response, id_option) VALUES
    (132, 116),
    (132, 117),
    (135, 119),
    (136, 122),
    (139, 124),
    (140, 127),
    (140, 128),
    (140, 129),
    (601, 401),
    (602, 404),
    (603, 406),
    (604, 407),
    (604, 409),
    (605, 411),
    (605, 413);

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (143, 82, NULL, 'Databases Hybrid Lesson Event', 'Relational model session', 'lesson',
     '2026-03-05 18:00:00', '2026-03-05 20:00:00', 0, 1, 30, 'active'),
    (144, 83, NULL, 'SQL Laboratory Event', 'In-person SQL session', 'lesson',
     '2026-04-10 18:00:00', '2026-04-10 20:00:00', 0, 1, 60, 'draft'),
    (145, 84, NULL, 'Online Safety Lesson Event', 'Online training', 'lesson',
     '2026-04-08 15:00:00', '2026-04-08 17:00:00', 0, 1, 20, 'active'),
    (146, 85, NULL, 'Evento Closed Laboratory RC', 'Completed historical event', 'lesson',
     '2026-03-15 09:00:00', '2026-03-15 11:00:00', 0, 0, NULL, 'completed'),
    (147, NULL, 94, 'Evento Databases Practical Exam', 'Practical exam period', 'assessment',
     '2026-03-10 09:00:00', '2026-03-10 11:00:00', 0, 1, 120, 'active'),
    (148, NULL, 96, 'Evento Safety Form', 'Form availability', 'assessment',
     '2026-04-09 08:00:00', '2026-04-12 22:00:00', 0, 1, 60, 'active'),
    (149, NULL, 97, 'Evento Networks Exam', 'Inactive exam event', 'assessment',
     '2026-05-20 09:00:00', '2026-05-20 11:00:00', 0, 0, NULL, 'inactive'),
    (150, NULL, NULL, 'Coordination Meeting', 'Cross-team follow-up meeting', 'meeting',
     '2026-03-12 12:00:00', '2026-03-12 13:00:00', 0, 1, 15, 'active'),
    (151, NULL, 98, 'Evento Complete Databases Assessment', 'Complete assessment availability', 'assessment',
     '2026-03-18 08:00:00', '2026-03-28 23:59:59', 0, 1, 120, 'active');

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (3, 143),
    (4, 143),
    (6, 143),
    (14, 143),
    (15, 143),
    (3, 144),
    (4, 144),
    (14, 144),
    (15, 144),
    (3, 147),
    (4, 147),
    (14, 147),
    (15, 147),
    (12, 145),
    (12, 146),
    (15, 146),
    (12, 148),
    (15, 148),
    (15, 145),
    (3, 151),
    (4, 151),
    (6, 151),
    (14, 151),
    (15, 151),
    (3, 150),
    (4, 150),
    (6, 150),
    (15, 150),
    (14, 150),
    (6, 147);

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (143, 53),
    (144, 53),
    (145, 55),
    (146, 54),
    (147, 53),
    (148, 55),
    (150, 53),
    (151, 53);

INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (152, 82, 15, 'present', 'automatic', '2026-03-05 18:00:00', '2026-03-05 20:00:00', 'Attendance recorded automatically', 'active'),
    (153, 82, 4, 'absent', 'manual', NULL, NULL, 'Absence recorded by the teacher', 'active'),
    (154, 83, 15, 'partial', 'manual', '2026-04-10 18:30:00', '2026-04-10 19:20:00', 'Partial attendance', 'corrected'),
    (155, 84, 12, 'present', 'automatic', '2026-04-08 15:00:00', '2026-04-08 17:00:00', NULL, 'active'),
    (156, 84, 15, 'late', 'automatic', '2026-04-08 15:25:00', '2026-04-08 17:00:00', 'Late entry', 'active');

INSERT INTO absence_justification (
    id_absence_justification, id_attendance_record, id_user_student_submitter, id_user_processor,
    submitted_at, reason, attachment, processed_at, decision_notes, state
) VALUES
    (161, 153, 4, 1, '2026-03-06 09:00:00', 'Documented illness', '/justifications/161.pdf',
     '2026-03-07 10:00:00', 'Proof accepted', 'approved'),
    (162, 154, 15, 8, '2026-04-11 09:00:00', 'Early departure for personal reasons', NULL,
     '2026-04-12 10:00:00', 'Insufficient justification', 'rejected'),
    (163, 156, 15, NULL, '2026-04-09 09:00:00', 'Transport delay', NULL,
     NULL, NULL, 'under_review');

UPDATE attendance_record
SET status = 'justified',
    state = 'corrected',
    notes = 'Absence justified by approved request.'
WHERE id_attendance_record = 153;

INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, id_course_occurrence, title, type, released_at, state
) VALUES
    (171, 42, 300, 'Databases Final Grade Sheet', 'final', NULL, 'draft'),
    (172, 42, 300, 'Databases Exam Grade Sheet', 'exam', NULL, 'draft'),
    (173, 44, 330, 'Industrial Safety Grade Sheet', 'continuous_assessment', NULL, 'draft'),
    (174, 43, 321, 'Inactive Networks Grade Sheet', 'other', NULL, 'draft');

INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (171, 53),
    (172, 53),
    (173, 55),
    (174, 54);

INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (171, 94, 80.00),
    (171, 95, 20.00),
    (172, 94, 100.00),
    (173, 96, 100.00),
    (174, 97, 100.00);

INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes
) VALUES
    (182, 171, 15, 122, 'AUTO-171-15', 16.00, 'approved', '2026-06-30 12:10:00', 'Automatic final grade snapshot'),
    (183, 171, 4, NULL, 'AUTO-171-4', 0.00, 'absent', '2026-06-30 12:15:00', 'Automatic final grade snapshot'),
    (184, 172, 15, 125, 'AUTO-172-15', 0.00, 'failed', '2026-03-15 12:10:00', 'Automatic final grade snapshot'),
    (185, 173, 12, 124, 'AUTO-173-12', 15.00, 'approved', '2026-04-15 12:00:00', 'Automatic final grade snapshot'),
    (186, 174, 15, 126, 'AUTO-174-15', 12.00, 'approved', '2026-05-30 12:00:00', 'Automatic final grade snapshot'),
    (187, 171, 7, NULL, 'AUTO-171-7', 9.00, 'pending', '2026-06-30 12:20:00', 'Automatic final grade snapshot');

INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, validation_code, issued_at, state, final_grade
) VALUES
    (195, 33, 330, 12, 'Safety Qualification Certificate', 'Business qualification completed', 'qualification', 'template-company', NULL, NULL, 'draft', NULL),
    (196, 30, 300, 15, 'Replacement Databases Certificate', 'Replacement document', 'completion', 'template-v2', NULL, NULL, 'draft', NULL);

INSERT INTO management_view (
    id_management_view, title, type, description, visibility_scope, state
) VALUES
    (200, 'Global Administration Dashboard', 'dashboard', 'Global system indicators', 'GLOBAL', 'active'),
    (201, 'ISG Organization Dashboard', 'dashboard', 'ISG organization indicators', 'ORGANIZATION', 'active'),
    (202, 'LEI Course Dashboard', 'dashboard', 'LEI course indicators', 'COURSE', 'active'),
    (203, 'Databases Subject Dashboard', 'dashboard', 'Database indicators', 'SUBJECT', 'active'),
    (204, 'BD-T1 Class Group Dashboard', 'dashboard', 'Class group attendance indicators', 'CLASS_GROUP', 'active'),
    (205, 'Inactive Dashboard', 'dashboard', 'Old view kept for historical records', 'GLOBAL', 'inactive');

INSERT INTO access_management_view (id_user, id_management_view) VALUES
    (1, 200),
    (8, 201),
    (10, 202),
    (14, 203),
    (3, 204),
    (15, 204),
    (1, 205);

INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (211, 'BD-T1 Class Group', 'class_group', 'participants', '2026-03-01 08:00:00', 'active'),
    (212, 'Databases Practical Exam', 'assessment', 'participants', '2026-03-08 08:00:00', 'active'),
    (213, 'ISG Announcements', 'organization', 'organization', '2026-03-01 08:30:00', 'active'),
    (214, 'Inactive Channel', 'other', 'private', '2026-03-01 08:45:00', 'inactive'),
    (215, 'SI Safety Block', 'content_block', 'participants', '2026-04-01 08:00:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (3, 211, 'teacher', '2026-03-01 08:00:00', 0, 'active'),
    (4, 211, 'student', '2026-03-01 08:00:00', 0, 'active'),
    (15, 211, 'student', '2026-03-01 08:00:00', 0, 'active'),
    (3, 212, 'teacher', '2026-03-08 08:00:00', 0, 'active'),
    (15, 212, 'student', '2026-03-08 08:00:00', 0, 'active'),
    (1, 213, 'administrator', '2026-03-01 08:30:00', 0, 'active'),
    (8, 213, 'administrator', '2026-03-01 08:30:00', 1, 'active'),
    (12, 215, 'teacher', '2026-04-01 08:00:00', 0, 'active'),
    (15, 215, 'student', '2026-04-01 08:00:00', 0, 'active');

INSERT INTO associate_channel_class_group (id_channel, id_class_group) VALUES
    (211, 53),
    (212, 53),
    (215, 55);

INSERT INTO associate_channel_content_block (id_channel, id_content_block) VALUES
    (211, 63),
    (215, 65);

INSERT INTO associate_channel_assessment (id_channel, id_assessment) VALUES
    (212, 94),
    (215, 96);

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (225, 211, 3, NULL, 143, 'Material BD-T1', 'Material published for the next lesson', 'announcement',
     'normal', NULL, '2026-03-04 18:00:00', NULL, NULL, '2026-03-04 18:05:00', 'sent'),
    (226, 211, 15, 225, NULL, 'Re: Material BD-T1', 'I confirm receipt of the material', 'comment',
     'low', NULL, '2026-03-04 19:00:00', NULL, NULL, '2026-03-04 19:00:30', 'sent'),
    (227, 212, 3, NULL, 147, 'Databases Exam Notice', 'The practical exam starts at 09:00', 'warning',
     'urgent', NULL, '2026-03-09 12:00:00', NULL, NULL, '2026-03-09 12:01:00', 'sent'),
    (228, 213, 1, NULL, NULL, 'General Announcement', 'Administrative procedure update', 'notification',
     'normal', NULL, '2026-03-02 09:00:00', NULL, NULL, '2026-03-02 09:05:00', 'sent'),
    (229, 215, 12, NULL, 145, 'Safety Attachment', 'Support file attached', 'attachment',
     'high', '/messages/229/normas.pdf', '2026-04-07 10:00:00', NULL, NULL, '2026-04-07 10:02:00', 'sent'),
    (230, 211, 3, NULL, NULL, 'Scheduled Databases Message', 'Automatic reminder for review', 'reminder',
     'normal', NULL, '2026-03-05 08:00:00', NULL, '2026-03-06 08:00:00', NULL, 'scheduled'),
    (231, 211, 3, NULL, NULL, 'Teacher Draft', 'Message in preparation', 'text',
     NULL, NULL, '2026-03-05 09:00:00', NULL, NULL, NULL, 'draft'),
    (232, 213, NULL, NULL, NULL, 'System Message', 'Automatic process completed', 'system',
     'normal', NULL, '2026-03-05 10:00:00', NULL, NULL, '2026-03-05 10:00:10', 'sent'),
    (233, 211, 3, NULL, NULL, 'Inactive Message', 'Old channel content', 'other',
     'low', NULL, '2026-03-01 10:00:00', '2026-03-02 10:00:00', NULL, '2026-03-01 10:05:00', 'deleted');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, state
) VALUES
    (15, 225, '2026-03-04 18:05:20', '2026-03-04 18:20:00', 'read'),
    (4, 225, '2026-03-04 18:05:20', NULL, 'delivered'),
    (3, 226, '2026-03-04 19:00:45', '2026-03-04 19:10:00', 'read'),
    (15, 227, '2026-03-09 12:01:20', NULL, 'delivered'),
    (8, 228, NULL, NULL, 'pending'),
    (15, 229, NULL, NULL, 'pending'),
    (15, 230, NULL, NULL, 'pending'),
    (1, 232, '2026-03-05 10:00:20', NULL, 'delivered');

-- Demo direct user conversations for the Messages page
INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (3200, 'Admin User and Teacher User', 'message', 'participants', '2026-07-03 14:00:00', 'active'),
    (3201, 'Admin User and Coordinator User', 'message', 'participants', '2026-07-03 14:02:00', 'active'),
    (3202, 'Teacher User and Student User', 'message', 'participants', '2026-07-03 14:04:00', 'active'),
    (3203, 'Teacher Two and Student Two', 'message', 'participants', '2026-07-03 14:06:00', 'active'),
    (3204, 'Admin User and Student User', 'message', 'participants', '2026-07-03 14:08:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (1, 3200, 'owner', '2026-07-03 14:00:00', 0, 'active'),
    (3, 3200, 'member', '2026-07-03 14:00:00', 0, 'active'),
    (1, 3201, 'owner', '2026-07-03 14:02:00', 0, 'active'),
    (2, 3201, 'member', '2026-07-03 14:02:00', 0, 'active'),
    (3, 3202, 'owner', '2026-07-03 14:04:00', 0, 'active'),
    (4, 3202, 'member', '2026-07-03 14:04:00', 0, 'active'),
    (6, 3203, 'owner', '2026-07-03 14:06:00', 0, 'active'),
    (7, 3203, 'member', '2026-07-03 14:06:00', 0, 'active'),
    (1, 3204, 'owner', '2026-07-03 14:08:00', 0, 'active'),
    (4, 3204, 'member', '2026-07-03 14:08:00', 0, 'active');

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (3200, 3200, 1, NULL, NULL, NULL, 'Can you review the PRJ-T1 attendance notes today?', 'text',
     'normal', NULL, '2026-07-03 14:10:00', NULL, NULL, '2026-07-03 14:10:10', 'sent'),
    (3201, 3200, 3, NULL, NULL, NULL, 'Yes, I will update the missing entries before lunch.', 'text',
     'normal', NULL, '2026-07-03 14:11:00', NULL, NULL, '2026-07-03 14:11:10', 'sent'),
    (3202, 3200, 1, NULL, NULL, NULL, 'Thanks. Please also flag students with repeated late arrivals.', 'text',
     'normal', NULL, '2026-07-03 14:12:00', NULL, NULL, '2026-07-03 14:12:10', 'sent'),
    (3203, 3200, 3, NULL, NULL, NULL, 'Done. I left two notes for follow-up.', 'text',
     'normal', NULL, '2026-07-03 14:18:00', NULL, NULL, '2026-07-03 14:18:10', 'sent'),
    (3204, 3201, 2, NULL, NULL, NULL, 'The subject coordination report is ready for validation.', 'text',
     'normal', NULL, '2026-07-03 14:13:00', NULL, NULL, '2026-07-03 14:13:10', 'sent'),
    (3205, 3201, 1, NULL, NULL, NULL, 'I will review it this afternoon.', 'text',
     'normal', NULL, '2026-07-03 14:14:00', NULL, NULL, '2026-07-03 14:14:10', 'sent'),
    (3206, 3202, 3, NULL, NULL, NULL, 'Your project proposal needs a clearer scope section.', 'text',
     'normal', NULL, '2026-07-03 14:15:00', NULL, NULL, '2026-07-03 14:15:10', 'sent'),
    (3207, 3202, 4, NULL, NULL, NULL, 'I updated the scope and uploaded the new draft.', 'text',
     'normal', NULL, '2026-07-03 14:16:00', NULL, NULL, '2026-07-03 14:16:10', 'sent'),
    (3208, 3202, 3, NULL, NULL, NULL, 'Good. Bring one example dataset to the next lesson.', 'text',
     'normal', NULL, '2026-07-03 14:17:00', NULL, NULL, '2026-07-03 14:17:10', 'sent'),
    (3209, 3203, 6, NULL, NULL, NULL, 'The online class link is active for tomorrow.', 'text',
     'normal', NULL, '2026-07-03 14:19:00', NULL, NULL, '2026-07-03 14:19:10', 'sent'),
    (3210, 3203, 7, NULL, NULL, NULL, 'Thank you, I can access it now.', 'text',
     'normal', NULL, '2026-07-03 14:20:00', NULL, NULL, '2026-07-03 14:20:10', 'sent'),
    (3211, 3203, 6, NULL, NULL, NULL, 'Great. Remember to complete the short exercise before class.', 'text',
     'normal', NULL, '2026-07-03 14:21:00', NULL, NULL, '2026-07-03 14:21:10', 'sent'),
    (3212, 3204, 4, NULL, NULL, NULL, 'I need help confirming my enrollment status.', 'text',
     'normal', NULL, '2026-07-03 14:22:00', NULL, NULL, '2026-07-03 14:22:10', 'sent'),
    (3213, 3204, 1, NULL, NULL, NULL, 'Your enrollment is active. I sent the confirmation to your profile.', 'text',
     'normal', NULL, '2026-07-03 14:23:00', NULL, NULL, '2026-07-03 14:23:10', 'sent');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, state
) VALUES
    (3, 3200, '2026-07-03 14:10:20', '2026-07-03 14:10:45', 'read'),
    (1, 3201, '2026-07-03 14:11:20', '2026-07-03 14:11:50', 'read'),
    (3, 3202, '2026-07-03 14:12:20', '2026-07-03 14:12:55', 'read'),
    (1, 3203, '2026-07-03 14:18:20', NULL, 'delivered'),
    (1, 3204, '2026-07-03 14:13:20', NULL, 'delivered'),
    (2, 3205, '2026-07-03 14:14:20', '2026-07-03 14:15:00', 'read'),
    (4, 3206, '2026-07-03 14:15:20', '2026-07-03 14:15:50', 'read'),
    (3, 3207, '2026-07-03 14:16:20', NULL, 'delivered'),
    (4, 3208, '2026-07-03 14:17:20', NULL, 'delivered'),
    (7, 3209, '2026-07-03 14:19:20', '2026-07-03 14:20:00', 'read'),
    (6, 3210, '2026-07-03 14:20:20', '2026-07-03 14:20:45', 'read'),
    (7, 3211, '2026-07-03 14:21:20', NULL, 'delivered'),
    (1, 3212, '2026-07-03 14:22:20', '2026-07-03 14:22:50', 'read'),
    (4, 3213, '2026-07-03 14:23:20', NULL, 'delivered');

-- Long direct demo chat with real downloadable attachments for the Messages page
INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (3205, 'Admin User and Teacher Two', 'message', 'participants', '2026-07-06 08:55:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (1, 3205, 'owner', '2026-07-06 08:55:00', 0, 'active'),
    (6, 3205, 'member', '2026-07-06 08:55:00', 0, 'active');

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
)
WITH RECURSIVE admin_teacher2_message_sequence(n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM admin_teacher2_message_sequence WHERE n < 99
)
SELECT
    4300 + n,
    3205,
    CASE WHEN MOD(n, 2) = 0 THEN 1 ELSE 6 END,
    NULL,
    NULL,
    CASE n
        WHEN 7 THEN 'er-model.webp'
        WHEN 14 THEN 'project-guide.pdf'
        WHEN 22 THEN 'normalization-demo.mp4'
        WHEN 31 THEN 'scorm-quality.zip'
        WHEN 39 THEN 'industrial-safety-slides.pdf'
        WHEN 47 THEN 'planning-video.mp4'
        WHEN 58 THEN 'xapi-audit.zip'
        WHEN 66 THEN 'er-diagram.webp'
        WHEN 78 THEN 'project-guide-final.pdf'
        WHEN 91 THEN 'planning-video-checklist.mp4'
        ELSE NULL
    END,
    CASE n
        WHEN 7 THEN 'Attached the ER model image so we can validate the course and class group relationships visually.'
        WHEN 14 THEN 'Attached the project guide PDF for the checklist review.'
        WHEN 22 THEN 'Attached the normalization demo video for the database review notes.'
        WHEN 31 THEN 'Attached the SCORM quality package so we can test repository downloads.'
        WHEN 39 THEN 'Attached the safety slides PDF for the training block validation.'
        WHEN 47 THEN 'Attached the planning video used in the online lesson demo.'
        WHEN 58 THEN 'Attached the xAPI audit package for the import validation scenario.'
        WHEN 66 THEN 'Attached the ER diagram again with the entity names visible.'
        WHEN 78 THEN 'Attached the final project guide PDF for approval.'
        WHEN 91 THEN 'Attached the planning video checklist for the last validation pass.'
        ELSE CONCAT(
            'Agenda item ',
            LPAD(n + 1, 2, '0'),
            ': ',
            CASE MOD(n, 12)
                WHEN 0 THEN 'Confirm the attendance notes before the weekly academic report.'
                WHEN 1 THEN 'Review the project class group status and flag any pending enrollment changes.'
                WHEN 2 THEN 'Check whether the latest uploaded material appears in the correct content block.'
                WHEN 3 THEN 'Validate that students with repeated late arrivals have follow-up notes.'
                WHEN 4 THEN 'Update the assessment checklist with the remaining correction tasks.'
                WHEN 5 THEN 'Compare the repository files with the lesson plan before publication.'
                WHEN 6 THEN 'Confirm the direct-message attachment download path with the demo account.'
                WHEN 7 THEN 'Keep the message thread focused on operational decisions and approvals.'
                WHEN 8 THEN 'Prepare the short summary for the coordinator after the validation pass.'
                WHEN 9 THEN 'Record any missing profile or class group information in the admin notes.'
                WHEN 10 THEN 'Check that read receipts and unread counters match the visible conversation state.'
                ELSE 'Close the resolved items and keep only open actions for the next review.'
            END
        )
    END,
    CASE WHEN n IN (7, 14, 22, 31, 39, 47, 58, 66, 78, 91) THEN 'attachment' ELSE 'text' END,
    'normal',
    CASE n
        WHEN 7 THEN 'contents/images/er.webp'
        WHEN 14 THEN 'contents/guide-prj.pdf'
        WHEN 22 THEN 'contents/videos/normalization.mp4'
        WHEN 31 THEN 'contents/packages/scorm-quality.zip'
        WHEN 39 THEN 'contents/presentations/industrial-safety-slides.pdf'
        WHEN 47 THEN 'contents/videos/planning.mp4'
        WHEN 58 THEN 'contents/packages/xapi-audit.zip'
        WHEN 66 THEN 'contents/images/er.webp'
        WHEN 78 THEN 'contents/guide-prj.pdf'
        WHEN 91 THEN 'contents/videos/planning.mp4'
        ELSE NULL
    END,
    DATE_ADD('2026-07-06 09:00:00', INTERVAL (n * 4) MINUTE),
    NULL,
    NULL,
    DATE_ADD(DATE_ADD('2026-07-06 09:00:00', INTERVAL (n * 4) MINUTE), INTERVAL 10 SECOND),
    'sent'
FROM admin_teacher2_message_sequence;

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, state
)
WITH RECURSIVE admin_teacher2_receipt_sequence(n) AS (
    SELECT 0
    UNION ALL
    SELECT n + 1 FROM admin_teacher2_receipt_sequence WHERE n < 99
)
SELECT
    CASE WHEN MOD(n, 2) = 0 THEN 6 ELSE 1 END,
    4300 + n,
    DATE_ADD(DATE_ADD('2026-07-06 09:00:00', INTERVAL (n * 4) MINUTE), INTERVAL 20 SECOND),
    CASE
        WHEN n IN (93, 94, 95, 96, 97, 98, 99) THEN NULL
        ELSE DATE_ADD(DATE_ADD('2026-07-06 09:00:00', INTERVAL (n * 4) MINUTE), INTERVAL 2 MINUTE)
    END,
    CASE WHEN n IN (93, 94, 95, 96, 97, 98, 99) THEN 'delivered' ELSE 'read' END
FROM admin_teacher2_receipt_sequence;

-- Final coverage for added features:
-- enrollment policies, content file processing,
-- class group visual preferences and historical enrollment states.

INSERT INTO class_group_enrollment_policy (id_class_group, approval_mode) VALUES
    (50, 'manual'),
    (51, 'auto_approve'),
    (53, 'manual'),
    (55, 'auto_approve');

UPDATE class_group
SET show_content_thumbnails = 1
WHERE id_class_group IN (51, 53, 55);

INSERT INTO content_file (
    id_content_file, id_content_item, original_filename, original_mime_type, final_mime_type,
    original_bytes, final_bytes, sha256, original_path, final_path, thumbnail_path,
    duration_seconds, width, height, page_count, processing_state, processing_error,
    created_at, processed_at
) VALUES
    (300, 74, 'er.webp', 'image/webp', 'image/webp',
     24556, 24556, '0418d158c1615be1e381d1ff37bf8aaf577532c5e3216f0d694eac2fd9b7fc40', NULL, 'contents/images/er.webp', 'contents/thumbs/er.webp',
     NULL, 1280, 720, NULL, 'ready', NULL, '2026-03-02 09:00:00', '2026-03-02 09:02:00'),
    (301, 75, 'normalization.mp4', 'video/mp4', 'video/mp4',
     42655, 42655, 'a1a8410841a09cc131ee75836134f019598e6b754dc7ad090e11a513a77b8d99', NULL, 'contents/videos/normalization.mp4', 'contents/thumbs/normalization.webp',
     3, 1280, 720, NULL, 'ready', NULL, '2026-03-02 09:10:00', '2026-03-02 09:18:00'),
    (302, 77, 'industrial-safety-slides.pdf', 'application/pdf', 'application/pdf',
     958, 958, '72fa759d077246cd28ca1ee7a643e807a8e4b8dcf9a43bbfb45366e9755ab014', NULL, 'contents/presentations/industrial-safety-slides.pdf', NULL,
     NULL, NULL, NULL, 1, 'ready', NULL, '2026-04-02 09:00:00', '2026-04-02 09:02:00'),
    (303, 78, 'scorm-quality.zip', 'application/zip', 'application/zip',
     718, 718, '3fff3833010a0e4b06e0bd51b0b6b40d8e20aef851ca49cd8aa70b76bb59b804', NULL, 'contents/packages/scorm-quality.zip', NULL,
     NULL, NULL, NULL, NULL, 'ready', NULL, '2026-04-02 09:15:00', '2026-04-02 09:20:00'),
    (305, 79, 'xapi-audit.zip', 'application/zip', 'application/zip',
     662, 662, '2a4037531c445614c9e17e9abf2a07bfe25a14c9b9d5aea2a4036b4fbf795274', NULL, 'contents/packages/xapi-audit.zip', NULL,
     NULL, NULL, NULL, NULL, 'ready', NULL, '2026-04-02 09:30:00', '2026-04-02 09:32:00'),
    (304, 81, 'review.m4a', 'audio/mp4', 'audio/mp4',
     230080, 230080, '3c8fc73b947ada2b40f4dc020894c71bb2043fafbdc63359d00e6b2ff9dc511f', NULL, 'contents/audio/review.m4a', NULL,
     21, NULL, NULL, NULL, 'ready', NULL, '2026-03-03 12:00:00', '2026-03-03 12:04:00');

INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (17, 'Student Four Enrollment Matrix', 'student4@gape.local', 'active', 'pt-PT', 'users/17/profile.webp',
     '2026-01-03 09:45:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-STD-017');

INSERT INTO student_profile (id_user, cod_student) VALUES
    (17, 'STD-017');

INSERT INTO grant_student (id_student_user, cod_permission) VALUES
    (17, 'VIEW_REPORTS');

INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date) VALUES
    (17, 30, 300, 'active', '2026-03-01', NULL),
    (17, 31, 310, 'completed', '2026-01-01', '2026-02-01'),
    (17, 32, 321, 'active', '2026-03-01', NULL),
    (17, 33, 330, 'inactive', '2026-04-01', NULL);

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (17, 53, 'pending', '2026-03-01', NULL),
    (17, 50, 'rejected', '2026-02-01', '2026-02-02'),
    (17, 51, 'completed', '2026-02-01', '2026-06-30'),
    (17, 54, 'inactive', '2026-03-01', NULL),
    (17, 56, 'inactive', '2025-02-01', '2025-06-30');

-- Event 142 occurred while this student was part of class group 51.
INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (17, 142);

-- Full-mode stress matrix for course, grade sheet and certificate edge cases.
-- The 1000+ id range is reserved for broad demo scenarios and regression checks.

INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (1000, 'Full Matrix Student Complete Ten Subjects', 'full.student1000@gape.local', 'active', 'pt-PT', 'users/1000/profile.webp',
     '2026-02-01 09:00:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1000'),
    (1001, 'Full Matrix Student Incomplete Ten Subjects', 'full.student1001@gape.local', 'active', 'pt-PT', 'users/1001/profile.webp',
     '2026-02-01 09:05:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1001'),
    (1002, 'Full Matrix Student Failed One', 'full.student1002@gape.local', 'active', 'pt-PT', 'users/1002/profile.webp',
     '2026-02-01 09:10:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1002'),
    (1003, 'Full Matrix Student Failed Two', 'full.student1003@gape.local', 'active', 'pt-PT', 'users/1003/profile.webp',
     '2026-02-01 09:15:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1003'),
    (1004, 'Full Matrix Student Passed One', 'full.student1004@gape.local', 'active', 'pt-PT', 'users/1004/profile.webp',
     '2026-02-01 09:20:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1004'),
    (1005, 'Full Matrix Student Passed Two', 'full.student1005@gape.local', 'active', 'pt-PT', 'users/1005/profile.webp',
     '2026-02-01 09:25:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1005'),
    (1006, 'Full Matrix Student Empty And Mismatch', 'full.student1006@gape.local', 'active', 'pt-PT', 'users/1006/profile.webp',
     '2026-02-01 09:30:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1006'),
    (1007, 'Full Matrix Student Hundred Point Scale', 'full.student1007@gape.local', 'active', 'pt-PT', 'users/1007/profile.webp',
     '2026-02-01 09:35:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-FULL-1007');

INSERT INTO student_profile (id_user, cod_student) VALUES
    (1000, 'STD-FULL-1000'),
    (1001, 'STD-FULL-1001'),
    (1002, 'STD-FULL-1002'),
    (1003, 'STD-FULL-1003'),
    (1004, 'STD-FULL-1004'),
    (1005, 'STD-FULL-1005'),
    (1006, 'STD-FULL-1006'),
    (1007, 'STD-FULL-1007');

INSERT INTO grant_student (id_student_user, cod_permission) VALUES
    (1000, 'VIEW_REPORTS'),
    (1001, 'VIEW_REPORTS'),
    (1002, 'VIEW_REPORTS'),
    (1003, 'VIEW_REPORTS'),
    (1004, 'VIEW_REPORTS'),
    (1005, 'VIEW_REPORTS'),
    (1006, 'VIEW_REPORTS'),
    (1007, 'VIEW_REPORTS');

INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, photo, description, ects,
    certificate_max_grade, duration, frequency, type, state
) VALUES
    (1000, 10, 20, 'Full Matrix Ten Subject Degree', 'TSD', NULL, 'Course with ten subjects and exact ECTS coverage', 60.00,
     20.00, '1', 'semester', 'degree', 'active'),
    (1001, 10, 20, 'Full Matrix Few Subject Course', 'FSC', NULL, 'Course with only two subjects', 12.00,
     20.00, '1', 'semester', 'short_course', 'active'),
    (1002, 10, 20, 'Full Matrix Empty Course', 'EMPTY', NULL, 'Course with no subjects to test empty curriculum handling', 30.00,
     20.00, '1', 'annual', 'other', 'active'),
    (1003, 10, 20, 'Full Matrix ECTS Mismatch Course', 'ECTSM', NULL, 'Course whose subject ECTS total does not match the course ECTS', 30.00,
     20.00, '1', 'semester', 'professional_training', 'active'),
    (1004, 10, 20, 'Full Matrix All Failed Course', 'FAILC', NULL, 'Single-subject course where every enrolled student failed', 6.00,
     20.00, '1', 'quadrimester', 'short_course', 'active'),
    (1005, 10, 20, 'Full Matrix All Passed Course', 'PASSC', NULL, 'Single-subject course where every enrolled student passed', 6.00,
     20.00, '1', 'quadrimester', 'short_course', 'active'),
    (1006, 10, 20, 'Full Matrix Draft And Empty Grades Course', 'NOGRD', NULL, 'Course with grade sheets without complete grade data', 12.00,
     20.00, '1', 'semester', 'other', 'active'),
    (1007, 10, 20, 'Full Matrix Hundred Point Certificate Course', 'HPC', NULL, 'Course using a 0 to 100 certificate and subject grading scale', 10.00,
     100.00, '1', 'annual', 'professional_training', 'active');

INSERT INTO course_period_template (
    id_course, curricular_year, term, starts_month, starts_day, ends_month, ends_day
) VALUES
    (1000, 1, 'semester_1', 1, 1, 6, 30),
    (1000, 1, 'semester_2', 7, 1, 12, 31),
    (1001, 1, 'semester_1', 1, 1, 6, 30),
    (1001, 1, 'semester_2', 7, 1, 12, 31),
    (1002, 1, 'annual', 1, 1, 12, 31),
    (1003, 1, 'semester_1', 1, 1, 6, 30),
    (1003, 1, 'semester_2', 7, 1, 12, 31),
    (1004, 1, 'quadrimester_1', 1, 1, 4, 30),
    (1004, 1, 'quadrimester_2', 5, 1, 8, 31),
    (1004, 1, 'quadrimester_3', 9, 1, 12, 31),
    (1005, 1, 'quadrimester_1', 1, 1, 4, 30),
    (1005, 1, 'quadrimester_2', 5, 1, 8, 31),
    (1005, 1, 'quadrimester_3', 9, 1, 12, 31),
    (1006, 1, 'semester_1', 1, 1, 6, 30),
    (1006, 1, 'semester_2', 7, 1, 12, 31),
    (1007, 1, 'annual', 1, 1, 12, 31);

INSERT INTO course_occurrence (
    id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
) VALUES
    (10000, 1000, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10010, 1001, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10020, 1002, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10030, 1003, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10040, 1004, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10050, 1005, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10060, 1006, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (10070, 1007, 2026, '2026', '2026-01-01', '2026-12-31', 'active');

INSERT INTO course_occurrence_period (
    id_course_occurrence_period, id_course_occurrence, curricular_year, term, starts_at, ends_at, state
) VALUES
    (100001, 10000, 1, 'semester_1', '2026-01-01', '2026-06-30', 'active'),
    (100002, 10000, 1, 'semester_2', '2026-07-01', '2026-12-31', 'active'),
    (100101, 10010, 1, 'semester_1', '2026-01-01', '2026-06-30', 'active'),
    (100102, 10010, 1, 'semester_2', '2026-07-01', '2026-12-31', 'active'),
    (100201, 10020, 1, 'annual', '2026-01-01', '2026-12-31', 'active'),
    (100301, 10030, 1, 'semester_1', '2026-01-01', '2026-06-30', 'active'),
    (100302, 10030, 1, 'semester_2', '2026-07-01', '2026-12-31', 'active'),
    (100401, 10040, 1, 'quadrimester_1', '2026-01-01', '2026-04-30', 'active'),
    (100402, 10040, 1, 'quadrimester_2', '2026-05-01', '2026-08-31', 'active'),
    (100403, 10040, 1, 'quadrimester_3', '2026-09-01', '2026-12-31', 'active'),
    (100501, 10050, 1, 'quadrimester_1', '2026-01-01', '2026-04-30', 'active'),
    (100502, 10050, 1, 'quadrimester_2', '2026-05-01', '2026-08-31', 'active'),
    (100503, 10050, 1, 'quadrimester_3', '2026-09-01', '2026-12-31', 'active'),
    (100601, 10060, 1, 'semester_1', '2026-01-01', '2026-06-30', 'active'),
    (100602, 10060, 1, 'semester_2', '2026-07-01', '2026-12-31', 'active'),
    (100701, 10070, 1, 'annual', '2026-01-01', '2026-12-31', 'active');

INSERT INTO subject (
    id_subject, id_organization, name, acronym, photo, description, ects, final_grade_max, workload_hours, state
) VALUES
    (1000, 10, 'Full Matrix Subject 01', 'TSD01', NULL, 'First subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1001, 10, 'Full Matrix Subject 02', 'TSD02', NULL, 'Second subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1002, 10, 'Full Matrix Subject 03', 'TSD03', NULL, 'Third subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1003, 10, 'Full Matrix Subject 04', 'TSD04', NULL, 'Fourth subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1004, 10, 'Full Matrix Subject 05', 'TSD05', NULL, 'Fifth subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1005, 10, 'Full Matrix Subject 06', 'TSD06', NULL, 'Sixth subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1006, 10, 'Full Matrix Subject 07', 'TSD07', NULL, 'Seventh subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1007, 10, 'Full Matrix Subject 08', 'TSD08', NULL, 'Eighth subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1008, 10, 'Full Matrix Subject 09', 'TSD09', NULL, 'Ninth subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1009, 10, 'Full Matrix Subject 10', 'TSD10', NULL, 'Tenth subject of the ten-subject course', 6.00, 20.00, 60, 'active'),
    (1010, 10, 'Full Matrix Small Foundations', 'FSF', NULL, 'First subject of a small course', 6.00, 20.00, 45, 'active'),
    (1011, 10, 'Full Matrix Small Practice', 'FSP', NULL, 'Second subject of a small course', 6.00, 20.00, 45, 'active'),
    (1012, 10, 'Full Matrix Mismatch Intro', 'ECTSI', NULL, 'Subject used in an ECTS mismatch course', 5.00, 20.00, 35, 'active'),
    (1013, 10, 'Full Matrix Mismatch Project', 'ECTSP', NULL, 'Second subject used in an ECTS mismatch course', 7.00, 20.00, 50, 'active'),
    (1014, 10, 'Full Matrix Failure Lab', 'FAIL', NULL, 'Subject where all enrolled students fail', 6.00, 20.00, 40, 'active'),
    (1015, 10, 'Full Matrix Pass Lab', 'PASS', NULL, 'Subject where all enrolled students pass', 6.00, 20.00, 40, 'active'),
    (1016, 10, 'Full Matrix Draft Grade Subject', 'DRAFTG', NULL, 'Subject with a draft grade sheet and no grade records', 6.00, 20.00, 40, 'active'),
    (1017, 10, 'Full Matrix Empty Class Subject', 'EMPTYG', NULL, 'Subject with an active class group and no students', 6.00, 20.00, 40, 'active'),
    (1018, 10, 'Full Matrix Hundred Point Subject', 'HPOINT', NULL, 'Subject graded on a 0 to 100 scale', 10.00, 100.00, 70, 'active');

INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory
) VALUES
    (1000, 1000, 1, 'semester_1', 1),
    (1000, 1001, 1, 'semester_1', 1),
    (1000, 1002, 1, 'semester_1', 1),
    (1000, 1003, 1, 'semester_1', 1),
    (1000, 1004, 1, 'semester_1', 1),
    (1000, 1005, 1, 'semester_2', 1),
    (1000, 1006, 1, 'semester_2', 1),
    (1000, 1007, 1, 'semester_2', 1),
    (1000, 1008, 1, 'semester_2', 1),
    (1000, 1009, 1, 'semester_2', 1),
    (1001, 1010, 1, 'semester_1', 1),
    (1001, 1011, 1, 'semester_2', 1),
    (1003, 1012, 1, 'semester_1', 1),
    (1003, 1013, 1, 'semester_2', 1),
    (1004, 1014, 1, 'quadrimester_1', 1),
    (1005, 1015, 1, 'quadrimester_1', 1),
    (1006, 1016, 1, 'semester_1', 1),
    (1006, 1017, 1, 'semester_2', 1),
    (1007, 1018, 1, 'annual', 1);

INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (1000, 1000, 1000, 10000, 100001, 'TSD01-A', 'onsite', 'active', 1, 35, '2026-01-01', '2026-06-30', 'morning'),
    (1001, 1001, 1000, 10000, 100001, 'TSD02-A', 'onsite', 'active', 1, 35, '2026-01-01', '2026-06-30', 'morning'),
    (1002, 1002, 1000, 10000, 100001, 'TSD03-A', 'hybrid', 'active', 1, 35, '2026-01-01', '2026-06-30', 'afternoon'),
    (1003, 1003, 1000, 10000, 100001, 'TSD04-A', 'hybrid', 'active', 1, 35, '2026-01-01', '2026-06-30', 'afternoon'),
    (1004, 1004, 1000, 10000, 100001, 'TSD05-A', 'online', 'active', 1, 35, '2026-01-01', '2026-06-30', 'evening'),
    (1005, 1005, 1000, 10000, 100002, 'TSD06-A', 'online', 'active', 1, 35, '2026-07-01', '2026-12-31', 'evening'),
    (1006, 1006, 1000, 10000, 100002, 'TSD07-A', 'onsite', 'active', 1, 35, '2026-07-01', '2026-12-31', 'morning'),
    (1007, 1007, 1000, 10000, 100002, 'TSD08-A', 'onsite', 'active', 1, 35, '2026-07-01', '2026-12-31', 'morning'),
    (1008, 1008, 1000, 10000, 100002, 'TSD09-A', 'hybrid', 'active', 1, 35, '2026-07-01', '2026-12-31', 'afternoon'),
    (1009, 1009, 1000, 10000, 100002, 'TSD10-A', 'hybrid', 'active', 1, 35, '2026-07-01', '2026-12-31', 'afternoon'),
    (1010, 1010, 1001, 10010, 100101, 'FSC-FND-A', 'online', 'active', 1, 25, '2026-01-01', '2026-06-30', 'evening'),
    (1011, 1011, 1001, 10010, 100102, 'FSC-PRC-A', 'online', 'active', 1, 25, '2026-07-01', '2026-12-31', 'evening'),
    (1012, 1012, 1003, 10030, 100301, 'ECTSM-INTRO', 'hybrid', 'active', 1, 25, '2026-01-01', '2026-06-30', 'mixed'),
    (1013, 1014, 1004, 10040, 100401, 'FAIL-ALL', 'onsite', 'active', 1, 20, '2026-01-01', '2026-04-30', 'morning'),
    (1014, 1015, 1005, 10050, 100501, 'PASS-ALL', 'onsite', 'active', 1, 20, '2026-01-01', '2026-04-30', 'afternoon'),
    (1015, 1016, 1006, 10060, 100601, 'DRAFT-NO-GRADES', 'online', 'active', 1, 20, '2026-01-01', '2026-06-30', 'evening'),
    (1016, 1017, 1006, 10060, 100602, 'EMPTY-CLASS', 'hybrid', 'active', 1, 20, '2026-07-01', '2026-12-31', 'mixed'),
    (1017, 1018, 1007, 10070, 100701, 'HPC-100', 'online', 'active', 1, 30, '2026-01-01', '2026-12-31', 'evening');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (3, 1000, 'active', '2026-02-02', NULL),
    (3, 1001, 'active', '2026-02-02', NULL),
    (3, 1002, 'active', '2026-02-02', NULL),
    (3, 1003, 'active', '2026-02-02', NULL),
    (3, 1004, 'active', '2026-02-02', NULL),
    (3, 1005, 'active', '2026-03-01', NULL),
    (3, 1006, 'active', '2026-03-01', NULL),
    (3, 1007, 'active', '2026-03-01', NULL),
    (3, 1008, 'active', '2026-03-01', NULL),
    (3, 1009, 'active', '2026-03-01', NULL),
    (6, 1010, 'active', '2026-02-10', NULL),
    (6, 1011, 'active', '2026-04-16', NULL),
    (12, 1012, 'active', '2026-02-10', NULL),
    (3, 1013, 'active', '2026-02-10', NULL),
    (3, 1014, 'active', '2026-02-10', NULL),
    (6, 1015, 'active', '2026-03-01', NULL),
    (6, 1016, 'active', '2026-03-01', NULL),
    (12, 1017, 'active', '2026-03-01', NULL);

INSERT INTO class_group_enrollment_policy (id_class_group, approval_mode) VALUES
    (1000, 'auto_approve'),
    (1001, 'auto_approve'),
    (1002, 'manual'),
    (1010, 'auto_approve'),
    (1011, 'auto_approve'),
    (1013, 'auto_approve'),
    (1014, 'auto_approve'),
    (1015, 'manual'),
    (1016, 'manual'),
    (1017, 'auto_approve');

INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date) VALUES
    (1000, 1000, 10000, 'active', '2026-01-01', '2026-12-31'),
    (1000, 1002, 10020, 'active', '2026-02-02', NULL),
    (1001, 1000, 10000, 'active', '2026-01-01', '2026-12-31'),
    (1001, 1003, 10030, 'active', '2026-01-01', '2026-06-30'),
    (1002, 1004, 10040, 'active', '2026-01-01', '2026-04-30'),
    (1002, 1006, 10060, 'active', '2026-01-01', '2026-06-30'),
    (1003, 1004, 10040, 'active', '2026-01-01', '2026-04-30'),
    (1004, 1001, 10010, 'active', '2026-01-01', '2026-12-31'),
    (1004, 1004, 10040, 'active', '2026-01-01', '2026-04-30'),
    (1004, 1005, 10050, 'active', '2026-01-01', '2026-04-30'),
    (1005, 1001, 10010, 'active', '2026-01-01', '2026-12-31'),
    (1005, 1005, 10050, 'active', '2026-01-01', '2026-04-30'),
    (1006, 1002, 10020, 'active', '2026-02-02', NULL),
    (1006, 1003, 10030, 'active', '2026-01-01', '2026-06-30'),
    (1006, 1006, 10060, 'active', '2026-01-01', '2026-06-30'),
    (1007, 1005, 10050, 'active', '2026-01-01', '2026-04-30'),
    (1007, 1007, 10070, 'active', '2026-01-01', '2026-12-31'),
    -- Mathematics 1 (occurrence 2025-2026): ten students, five completed.
    (4, 34, 345, 'completed', '2025-09-01', '2026-06-30'),
    (7, 34, 345, 'completed', '2025-09-01', '2026-06-30'),
    (12, 34, 345, 'completed', '2025-09-01', '2026-06-30'),
    (15, 34, 345, 'completed', '2025-09-01', '2026-06-30'),
    (17, 34, 345, 'completed', '2025-09-01', '2026-06-30'),
    (1000, 34, 345, 'active', '2025-09-01', '2026-08-01'),
    (1001, 34, 345, 'active', '2025-09-01', '2026-08-01'),
    (1002, 34, 345, 'active', '2025-09-01', '2026-08-01'),
    (1003, 34, 345, 'active', '2025-09-01', '2026-08-01'),
    (1004, 34, 345, 'active', '2025-09-01', '2026-08-01');

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (1000, 1000, 'active', '2026-02-02', NULL),
    (1000, 1001, 'active', '2026-02-02', NULL),
    (1000, 1002, 'active', '2026-02-02', NULL),
    (1000, 1003, 'active', '2026-02-02', NULL),
    (1000, 1004, 'active', '2026-02-02', NULL),
    (1000, 1005, 'active', '2026-07-01', NULL),
    (1000, 1006, 'active', '2026-07-01', NULL),
    (1000, 1007, 'active', '2026-07-01', NULL),
    (1000, 1008, 'active', '2026-07-01', NULL),
    (1000, 1009, 'active', '2026-07-01', NULL),
    (1001, 1000, 'active', '2026-02-02', NULL),
    (1001, 1001, 'active', '2026-02-02', NULL),
    (1001, 1002, 'active', '2026-02-02', NULL),
    (1001, 1012, 'active', '2026-02-10', NULL),
    (1002, 1013, 'active', '2026-02-10', NULL),
    (1002, 1015, 'active', '2026-03-01', NULL),
    (1003, 1013, 'active', '2026-02-10', NULL),
    (1004, 1010, 'active', '2026-02-10', NULL),
    (1004, 1011, 'active', '2026-07-01', NULL),
    (1004, 1013, 'active', '2026-02-10', NULL),
    (1004, 1014, 'active', '2026-02-10', NULL),
    (1005, 1010, 'active', '2026-02-10', NULL),
    (1005, 1011, 'active', '2026-07-01', NULL),
    (1005, 1014, 'active', '2026-02-10', NULL),
    (1006, 1012, 'active', '2026-02-10', NULL),
    (1006, 1015, 'active', '2026-03-01', NULL),
    (1007, 1014, 'active', '2026-02-10', NULL),
    (1007, 1017, 'active', '2026-03-01', NULL);

-- Realistic regular cohort: most completed class groups contain students and
-- academic evidence.  The two pending corrections are deliberate minority
-- cases: the grade sheet keeps those values as '-' instead of inventing zeroes.
INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (4, 57, 'completed', '2026-03-01', '2026-06-30'),
    (7, 58, 'completed', '2026-03-01', '2026-06-30'),
    (12, 59, 'completed', '2026-03-01', '2026-06-30'),
    (15, 60, 'completed', '2026-03-01', '2026-06-30'),
    (17, 60, 'completed', '2026-03-01', '2026-06-30'),
    (1000, 61, 'active', '2026-03-01', '2026-08-01'),
    (1001, 62, 'active', '2026-03-01', '2026-08-01'),
    (1002, 63, 'active', '2026-03-01', '2026-08-01'),
    (1003, 64, 'active', '2026-03-01', '2026-08-01');

-- Assessment enrollment is active while the historic attempts are recorded;
-- it is marked completed afterwards, just as a normal finished assessment is.
INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (4, 4000, 'active'),
    (7, 4000, 'active'),
    (12, 4000, 'active'),
    (15, 4000, 'active'),
    (17, 4000, 'active');

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (4000, 4, 4000, 1, 16.00, 'corrected', '2026-05-22 09:00:00', '2026-05-22 10:12:00'),
    (4001, 7, 4000, 1, 11.50, 'corrected', '2026-05-22 09:00:00', '2026-05-22 10:19:00'),
    (4002, 12, 4000, 1, NULL, 'submitted', '2026-05-22 09:00:00', '2026-05-22 10:08:00'),
    (4003, 15, 4000, 1, NULL, 'submitted', '2026-05-22 09:00:00', '2026-05-22 10:27:00'),
    (4004, 17, 4000, 1, 18.00, 'corrected', '2026-05-22 09:00:00', '2026-05-22 10:04:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (4000, 4000, 4000, 'RC-FIN-4', 'IP', NULL, 16.00, '2026-05-22 10:12:00'),
    (4001, 4001, 4000, 'RC-FIN-7', 'IP', NULL, 11.50, '2026-05-22 10:19:00'),
    (4002, 4002, 4000, 'RC-FIN-12', 'TCP/IP', NULL, NULL, '2026-05-22 10:08:00'),
    (4003, 4003, 4000, 'RC-FIN-15', 'IP', NULL, NULL, '2026-05-22 10:27:00'),
    (4004, 4004, 4000, 'RC-FIN-17', 'IP', NULL, 18.00, '2026-05-22 10:04:00');

UPDATE enroll_assessment
SET state = 'completed'
WHERE id_assessment = 4000
  AND id_student_user IN (4, 7, 12, 15, 17);

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (4000, NULL, 4000, 'Network Foundations Final',
     'Completed shared final assessment for the Information Systems Master Computer Networks cohort.', 'assessment',
     '2026-05-22 09:00:00', '2026-05-22 11:00:00', 0, 0, NULL, 'completed'),
    (4001, NULL, 4001, 'Network Services Review',
     'Scheduled review assessment for active Computer Networks class groups.', 'assessment',
     '2026-07-22 09:00:00', '2026-07-22 11:00:00', 0, 1, 120, 'active');

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (4000, 57),
    (4000, 58),
    (4000, 59),
    (4000, 60),
    (4001, 61),
    (4001, 62),
    (4001, 63),
    (4001, 64);

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (4, 4000),
    (7, 4000),
    (12, 4000),
    (15, 4000),
    (17, 4000),
    (12, 4001),
    (1000, 4001),
    (1001, 4001),
    (1002, 4001),
    (1003, 4001);

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    state
) VALUES
    (1000, 1013, 'FAIL-EVAL', 'Failure Lab Assessments', 'Assessments for the all-failed class', 1, 'active'),
    (1001, 1014, 'PASS-EVAL', 'Pass Lab Assessments', 'Assessments for the all-passed class', 1, 'active'),
    (1002, 1015, 'DRAFT-EVAL', 'Draft Grade Assessments', 'Assessments without configured grade sheet weights', 1, 'active'),
    (1003, 1016, 'EMPTY-EVAL', 'Empty Class Assessments', 'Assessments with incomplete total weight', 1, 'active'),
    (1004, 1017, 'HPC-EVAL', 'Hundred Point Assessment', 'Assessment for 0 to 100 grade scale', 1, 'active');

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
    max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode, state, available_from, available_until, order_no
) VALUES
    (1000, 1014, 1000, NULL, 'Failure Lab Midterm', 'Midterm assessment where every student failed', 'test', 'online', 'manual',
     20.00, 9.50, 50.00, 1, 'auto_approve', 'active', '2026-03-01 09:00:00', '2026-03-01 11:00:00', 1),
    (1001, 1014, 1000, 'SALA-EX03', 'Failure Lab Final', 'Final assessment where every student failed', 'exam', 'onsite', 'manual',
     20.00, 9.50, 50.00, 1, 'auto_approve', 'active', '2026-04-10 09:00:00', '2026-04-10 11:00:00', 2),
    (1002, 1015, 1001, NULL, 'Pass Lab Project', 'Project assessment where every student passed', 'test', 'online', 'manual',
     20.00, 9.50, 60.00, 1, 'auto_approve', 'active', '2026-03-01 14:00:00', '2026-03-01 16:00:00', 1),
    (1003, 1015, 1001, 'SALA-EX04', 'Pass Lab Final', 'Final assessment where every student passed', 'exam', 'onsite', 'manual',
     20.00, 9.50, 40.00, 1, 'auto_approve', 'active', '2026-04-10 14:00:00', '2026-04-10 16:00:00', 2),
    (1004, 1016, 1002, NULL, 'Draft Grades Quiz', 'Assessment without grade sheet weights yet', 'test', 'online', 'automatic',
     20.00, 9.50, 50.00, 1, 'manual', 'active', '2026-04-01 10:00:00', '2026-04-01 11:00:00', 1),
    (1005, 1016, 1002, NULL, 'Draft Grades Project', 'Second assessment without grade sheet weights yet', 'test', 'online', 'manual',
     20.00, 9.50, 50.00, 1, 'manual', 'active', '2026-04-15 10:00:00', '2026-04-15 12:00:00', 2),
    (1006, 1017, 1003, NULL, 'Empty Class Quiz', 'Assessment with partial weight configured', 'test', 'online', 'automatic',
     20.00, 9.50, 30.00, 1, 'auto_approve', 'active', '2026-07-10 18:00:00', '2026-07-10 19:00:00', 1),
    (1007, 1017, 1003, 'SALA-EX05', 'Empty Class Final', 'Assessment missing a complementary weight', 'exam', 'onsite', 'manual',
     20.00, 9.50, 0.00, 1, 'auto_approve', 'active', '2026-07-20 18:00:00', '2026-07-20 20:00:00', 2),
    (1008, 1018, 1004, NULL, 'Hundred Point Capstone', 'Assessment using the 0 to 100 scale', 'test', 'online', 'manual',
     100.00, 50.00, 100.00, 1, 'auto_approve', 'active', '2026-04-20 18:00:00', '2026-04-20 20:00:00', 1),
    (1100, 1000, NULL, 'SALA-EX06', 'TSD Subject 01 Final Assessment', 'Weighted final assessment for TSD subject 01', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-06-30 09:00:00', '2026-06-30 11:00:00', 1),
    (1101, 1001, NULL, 'SALA-EX07', 'TSD Subject 02 Final Assessment', 'Weighted final assessment for TSD subject 02', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-06-30 09:00:00', '2026-06-30 11:00:00', 1),
    (1102, 1002, NULL, 'SALA-EX08', 'TSD Subject 03 Final Assessment', 'Weighted final assessment for TSD subject 03', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-06-30 09:00:00', '2026-06-30 11:00:00', 1),
    (1103, 1003, NULL, 'SALA-EX09', 'TSD Subject 04 Final Assessment', 'Weighted final assessment for TSD subject 04', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-06-30 09:00:00', '2026-06-30 11:00:00', 1),
    (1104, 1004, NULL, 'SALA-EX10', 'TSD Subject 05 Final Assessment', 'Weighted final assessment for TSD subject 05', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-06-30 09:00:00', '2026-06-30 11:00:00', 1),
    (1105, 1005, NULL, 'SALA-EX06', 'TSD Subject 06 Final Assessment', 'Weighted final assessment for TSD subject 06', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-07-15 09:00:00', '2026-07-15 11:00:00', 1),
    (1106, 1006, NULL, 'SALA-EX07', 'TSD Subject 07 Final Assessment', 'Weighted final assessment for TSD subject 07', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-07-15 09:00:00', '2026-07-15 11:00:00', 1),
    (1107, 1007, NULL, 'SALA-EX08', 'TSD Subject 08 Final Assessment', 'Weighted final assessment for TSD subject 08', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-07-15 09:00:00', '2026-07-15 11:00:00', 1),
    (1108, 1008, NULL, 'SALA-EX09', 'TSD Subject 09 Final Assessment', 'Weighted final assessment for TSD subject 09', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-07-15 09:00:00', '2026-07-15 11:00:00', 1),
    (1109, 1009, NULL, 'SALA-EX10', 'TSD Subject 10 Final Assessment', 'Weighted final assessment for TSD subject 10', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-07-15 09:00:00', '2026-07-15 11:00:00', 1),
    (1110, 1010, NULL, 'SALA-EX11', 'Few Subject Foundations Final Assessment', 'Weighted final assessment for few-subject foundations', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-04-15 09:00:00', '2026-04-15 11:00:00', 1),
    (1111, 1011, NULL, 'SALA-EX11', 'Few Subject Practice Final Assessment', 'Weighted final assessment for few-subject practice', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-07-15 09:00:00', '2026-07-15 11:00:00', 1),
    (1112, 1012, NULL, 'SALA-EX12', 'ECTS Mismatch Intro Final Assessment', 'Weighted final assessment for ECTS mismatch intro', 'exam', 'onsite', 'manual',
     20.00, 9.50, 100.00, 1, 'auto_approve', 'active', '2026-04-15 09:00:00', '2026-04-15 11:00:00', 1);

INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (1100, 1000),
    (1101, 1001),
    (1102, 1002),
    (1103, 1003),
    (1104, 1004),
    (1105, 1005),
    (1106, 1006),
    (1107, 1007),
    (1108, 1008),
    (1109, 1009),
    (1110, 1010),
    (1111, 1011),
    (1112, 1012);

INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, id_course_occurrence, title, type, max_grade, passing_grade, weight_alert, released_at, state
) VALUES
    (1000, 1000, 10000, 'TSD Subject 01 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-06-30 10:00:00', 'published'),
    (1001, 1001, 10000, 'TSD Subject 02 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-06-30 10:00:00', 'published'),
    (1002, 1002, 10000, 'TSD Subject 03 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-06-30 10:00:00', 'published'),
    (1003, 1003, 10000, 'TSD Subject 04 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-06-30 10:00:00', 'published'),
    (1004, 1004, 10000, 'TSD Subject 05 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-06-30 10:00:00', 'published'),
    (1005, 1005, 10000, 'TSD Subject 06 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-07-15 10:00:00', 'published'),
    (1006, 1006, 10000, 'TSD Subject 07 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-07-15 10:00:00', 'published'),
    (1007, 1007, 10000, 'TSD Subject 08 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-07-15 10:00:00', 'published'),
    (1008, 1008, 10000, 'TSD Subject 09 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-07-15 10:00:00', 'published'),
    (1009, 1009, 10000, 'TSD Subject 10 Final Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-07-15 10:00:00', 'published'),
    (1010, 1010, 10010, 'Few Subject Foundations Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-04-15 10:00:00', 'published'),
    (1011, 1011, 10010, 'Few Subject Practice Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-06-30 10:00:00', 'published'),
    (1012, 1012, 10030, 'ECTS Mismatch Intro Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-04-15 10:00:00', 'published'),
    (1013, 1014, 10040, 'All Failed Lab Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-04-15 10:00:00', 'published'),
    (1014, 1015, 10050, 'All Passed Lab Grade Sheet', 'final', 20.00, 9.50, NULL, '2026-04-15 10:00:00', 'published'),
    (1015, 1016, 10060, 'Ended Grade Sheet Missing Grades Draft', 'continuous_assessment', 20.00, 9.50,
     'Assessment weights total 100%, but assessment grades are still missing.', NULL, 'draft'),
    (1016, 1017, 10060, 'Empty Class Partial Weight Grade Sheet', 'continuous_assessment', 20.00, 9.50,
     'Assessment weights total 30%. If this is not regularized before the class group period ends, the system will redistribute the weights equally so the sum is 100%.', NULL, 'draft'),
    (1017, 1018, 10070, 'Hundred Point Final Grade Sheet', 'final', 100.00, 50.00, NULL, '2026-05-30 10:00:00', 'published');

INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (1000, 1000),
    (1001, 1001),
    (1002, 1002),
    (1003, 1003),
    (1004, 1004),
    (1005, 1005),
    (1006, 1006),
    (1007, 1007),
    (1008, 1008),
    (1009, 1009),
    (1010, 1010),
    (1011, 1011),
    (1012, 1012),
    (1013, 1013),
    (1014, 1014),
    (1015, 1015),
    (1016, 1016),
    (1017, 1017);

INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (1000, 1100, 100.00),
    (1001, 1101, 100.00),
    (1002, 1102, 100.00),
    (1003, 1103, 100.00),
    (1004, 1104, 100.00),
    (1005, 1105, 100.00),
    (1006, 1106, 100.00),
    (1007, 1107, 100.00),
    (1008, 1108, 100.00),
    (1009, 1109, 100.00),
    (1010, 1110, 100.00),
    (1011, 1111, 100.00),
    (1012, 1112, 100.00),
    (1013, 1000, 50.00),
    (1013, 1001, 50.00),
    (1014, 1002, 60.00),
    (1014, 1003, 40.00),
    (1015, 1004, 50.00),
    (1015, 1005, 50.00),
    (1016, 1006, 30.00),
    (1016, 1007, 0.00),
    (1017, 1008, 100.00);

INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (1000, 1100, 'active'),
    (1000, 1101, 'active'),
    (1000, 1102, 'active'),
    (1000, 1103, 'active'),
    (1000, 1104, 'active'),
    (1000, 1105, 'active'),
    (1000, 1106, 'active'),
    (1000, 1107, 'active'),
    (1000, 1108, 'active'),
    (1000, 1109, 'active'),
    (1001, 1100, 'active'),
    (1001, 1101, 'active'),
    (1001, 1102, 'active'),
    (1001, 1112, 'active'),
    (1004, 1110, 'active'),
    (1004, 1111, 'active'),
    (1005, 1110, 'active'),
    (1005, 1111, 'active'),
    (1006, 1112, 'active'),
    (1002, 1000, 'active'),
    (1002, 1001, 'active'),
    (1003, 1000, 'active'),
    (1003, 1001, 'active'),
    (1004, 1000, 'active'),
    (1004, 1001, 'active'),
    (1004, 1002, 'active'),
    (1004, 1003, 'active'),
    (1005, 1002, 'active'),
    (1005, 1003, 'active'),
    (1002, 1004, 'active'),
    (1002, 1005, 'active'),
    (1006, 1004, 'active'),
    (1006, 1005, 'active'),
    (1007, 1002, 'active'),
    (1007, 1003, 'active'),
    (1007, 1008, 'active');

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (2100, 1000, 1100, 1, 14.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2101, 1000, 1101, 1, 16.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2102, 1000, 1102, 1, 12.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2103, 1000, 1103, 1, 18.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2104, 1000, 1104, 1, 15.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2105, 1000, 1105, 1, 13.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2106, 1000, 1106, 1, 17.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2107, 1000, 1107, 1, 11.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2108, 1000, 1108, 1, 19.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2109, 1000, 1109, 1, 16.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2110, 1001, 1100, 1, 11.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2111, 1001, 1101, 1, 10.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2112, 1001, 1102, 1, 8.00, 'corrected', '2026-06-30 09:00:00', '2026-06-30 10:30:00'),
    (2113, 1004, 1110, 1, 13.00, 'corrected', '2026-04-15 09:00:00', '2026-04-15 10:30:00'),
    (2114, 1004, 1111, 1, 16.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2115, 1005, 1110, 1, 10.00, 'corrected', '2026-04-15 09:00:00', '2026-04-15 10:30:00'),
    (2116, 1005, 1111, 1, 11.00, 'corrected', '2026-07-15 09:00:00', '2026-07-15 10:30:00'),
    (2117, 1001, 1112, 1, 14.00, 'corrected', '2026-04-15 09:00:00', '2026-04-15 10:30:00'),
    (2118, 1006, 1112, 1, 12.00, 'corrected', '2026-04-15 09:00:00', '2026-04-15 10:30:00'),
    (2000, 1002, 1000, 1, 2.00, 'corrected', '2026-03-01 09:00:00', '2026-03-01 10:40:00'),
    (2001, 1002, 1001, 1, 4.00, 'corrected', '2026-04-10 09:00:00', '2026-04-10 10:40:00'),
    (2002, 1003, 1000, 1, 6.00, 'corrected', '2026-03-01 09:00:00', '2026-03-01 10:40:00'),
    (2003, 1003, 1001, 1, 8.00, 'corrected', '2026-04-10 09:00:00', '2026-04-10 10:40:00'),
    (2004, 1004, 1000, 1, 8.00, 'corrected', '2026-03-01 09:00:00', '2026-03-01 10:40:00'),
    (2005, 1004, 1001, 1, 10.00, 'corrected', '2026-04-10 09:00:00', '2026-04-10 10:40:00'),
    (2006, 1004, 1002, 1, 12.00, 'corrected', '2026-03-01 14:00:00', '2026-03-01 15:40:00'),
    (2007, 1004, 1003, 1, 12.00, 'corrected', '2026-04-10 14:00:00', '2026-04-10 15:40:00'),
    (2008, 1005, 1002, 1, 15.00, 'corrected', '2026-03-01 14:00:00', '2026-03-01 15:40:00'),
    (2009, 1005, 1003, 1, 15.00, 'corrected', '2026-04-10 14:00:00', '2026-04-10 15:40:00'),
    (2010, 1007, 1002, 1, 18.00, 'corrected', '2026-03-01 14:00:00', '2026-03-01 15:40:00'),
    (2011, 1007, 1003, 1, 18.00, 'corrected', '2026-04-10 14:00:00', '2026-04-10 15:40:00'),
    (2013, 1002, 1004, 1, 14.00, 'corrected', '2026-04-01 10:00:00', '2026-04-01 10:50:00'),
    (2014, 1002, 1005, 1, 16.00, 'corrected', '2026-04-15 10:00:00', '2026-04-15 11:50:00'),
    (2012, 1007, 1008, 1, 87.00, 'corrected', '2026-04-20 18:00:00', '2026-04-20 19:40:00');

INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes
) VALUES
    (1000, 1000, 1000, NULL, 'AUTO-1000-1000', 14.00, 'approved', '2026-06-30 10:05:00', 'Ten-subject course complete record'),
    (1001, 1001, 1000, NULL, 'AUTO-1001-1000', 16.00, 'approved', '2026-06-30 10:06:00', 'Ten-subject course complete record'),
    (1002, 1002, 1000, NULL, 'AUTO-1002-1000', 12.00, 'approved', '2026-06-30 10:07:00', 'Ten-subject course complete record'),
    (1003, 1003, 1000, NULL, 'AUTO-1003-1000', 18.00, 'approved', '2026-06-30 10:08:00', 'Ten-subject course complete record'),
    (1004, 1004, 1000, NULL, 'AUTO-1004-1000', 15.00, 'approved', '2026-06-30 10:09:00', 'Ten-subject course complete record'),
    (1005, 1005, 1000, NULL, 'AUTO-1005-1000', 13.00, 'approved', '2026-07-15 10:05:00', 'Ten-subject course complete record'),
    (1006, 1006, 1000, NULL, 'AUTO-1006-1000', 17.00, 'approved', '2026-07-15 10:06:00', 'Ten-subject course complete record'),
    (1007, 1007, 1000, NULL, 'AUTO-1007-1000', 11.00, 'approved', '2026-07-15 10:07:00', 'Ten-subject course complete record'),
    (1008, 1008, 1000, NULL, 'AUTO-1008-1000', 19.00, 'approved', '2026-07-15 10:08:00', 'Ten-subject course complete record'),
    (1009, 1009, 1000, NULL, 'AUTO-1009-1000', 16.00, 'approved', '2026-07-15 10:09:00', 'Ten-subject course complete record'),
    (1010, 1000, 1001, NULL, 'AUTO-1000-1001', 11.00, 'approved', '2026-06-30 10:10:00', 'Incomplete certificate student has only partial subjects'),
    (1011, 1001, 1001, NULL, 'AUTO-1001-1001', 10.00, 'approved', '2026-06-30 10:11:00', 'Incomplete certificate student has only partial subjects'),
    (1012, 1002, 1001, NULL, 'AUTO-1002-1001', 8.00, 'failed', '2026-06-30 10:12:00', 'Incomplete certificate student failed one subject'),
    (1013, 1010, 1004, NULL, 'AUTO-1010-1004', 13.00, 'approved', '2026-04-15 10:05:00', 'Few-subject course approved record'),
    (1014, 1011, 1004, NULL, 'AUTO-1011-1004', 16.00, 'approved', '2026-06-30 10:05:00', 'Few-subject course approved record'),
    (1015, 1010, 1005, NULL, 'AUTO-1010-1005', 10.00, 'approved', '2026-04-15 10:06:00', 'Few-subject course low-pass record'),
    (1016, 1011, 1005, NULL, 'AUTO-1011-1005', 11.00, 'approved', '2026-06-30 10:06:00', 'Few-subject course low-pass record'),
    (1017, 1012, 1001, NULL, 'AUTO-1012-1001', 14.00, 'approved', '2026-04-15 10:05:00', 'ECTS mismatch course should not produce final course grade'),
    (1018, 1012, 1006, NULL, 'AUTO-1012-1006', 12.00, 'approved', '2026-04-15 10:06:00', 'ECTS mismatch course second student'),
    (1019, 1013, 1002, NULL, 'AUTO-1013-1002', 3.00, 'failed', '2026-04-15 10:05:00', 'All-failed class'),
    (1020, 1013, 1003, NULL, 'AUTO-1013-1003', 7.00, 'failed', '2026-04-15 10:06:00', 'All-failed class'),
    (1021, 1013, 1004, NULL, 'AUTO-1013-1004', 9.00, 'failed', '2026-04-15 10:07:00', 'All-failed class'),
    (1022, 1014, 1004, NULL, 'AUTO-1014-1004', 12.00, 'approved', '2026-04-15 10:05:00', 'All-passed class'),
    (1023, 1014, 1005, NULL, 'AUTO-1014-1005', 15.00, 'approved', '2026-04-15 10:06:00', 'All-passed class'),
    (1024, 1014, 1007, NULL, 'AUTO-1014-1007', 18.00, 'approved', '2026-04-15 10:07:00', 'All-passed class'),
    (1026, 1015, 1002, NULL, 'AUTO-1015-1002', 15.00, 'approved', '2026-04-15 12:05:00',
     'Draft mixed-completion class: this student has every assessment score, while another enrolled student still has missing grades.'),
    (1025, 1017, 1007, NULL, 'AUTO-1017-1007', 87.00, 'approved', '2026-05-30 10:05:00', 'Hundred-point grading scale');

INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, validation_code, issued_at, state, final_grade
) VALUES
    (1000, 1000, 10000, 1000, 'Ten Subject Degree Completion Certificate', 'Complete certificate using ten equal-ECTS subjects', 'completion',
     'template-ects-full', 'CERT-FULL-1000', '2026-07-20 09:00:00', 'issued', 15.10),
    (1001, 1000, 10000, 1001, 'Ten Subject Degree Incomplete Certificate', 'Incomplete certificate; not all subjects have approved final grades', 'completion',
     'template-ects-full', NULL, NULL, 'draft', NULL),
    (1002, 1001, 10010, 1004, 'Few Subject Course Certificate', 'Two-subject course completed with ECTS weighted final grade', 'completion',
     'template-ects-short', 'CERT-FULL-1002', '2026-07-01 09:00:00', 'issued', 14.50),
    (1003, 1001, 10010, 1005, 'Few Subject Low Pass Certificate', 'Two-subject course completed with low passing grades', 'completion',
     'template-ects-short', 'CERT-FULL-1003', '2026-07-01 09:10:00', 'issued', 10.50),
    (1004, 1004, 10040, 1002, 'All Failed Course Incomplete Certificate', 'Certificate exists but cannot be completed because the student failed', 'completion',
     'template-ects-short', NULL, NULL, 'draft', NULL),
    (1005, 1005, 10050, 1004, 'All Passed Course Certificate One', 'Single-subject course completed by all students', 'completion',
     'template-ects-short', 'CERT-FULL-1005', '2026-04-20 09:00:00', 'issued', 12.00),
    (1006, 1005, 10050, 1005, 'All Passed Course Certificate Two', 'Single-subject course completed by all students', 'completion',
     'template-ects-short', 'CERT-FULL-1006', '2026-04-20 09:10:00', 'issued', 15.00),
    (1007, 1005, 10050, 1007, 'All Passed Course Certificate Three', 'Single-subject course completed by all students', 'completion',
     'template-ects-short', 'CERT-FULL-1007', '2026-04-20 09:20:00', 'issued', 18.00),
    (1008, 1006, 10060, 1002, 'Draft Grades Incomplete Certificate', 'Certificate exists but grade sheets are draft or incomplete', 'completion',
     'template-ects-draft', NULL, NULL, 'draft', NULL),
    (1009, 1002, 10020, 1006, 'Empty Course Incomplete Certificate', 'Course has no subjects, so the certificate cannot be completed', 'completion',
     'template-ects-empty', NULL, NULL, 'draft', NULL),
    (1010, 1003, 10030, 1006, 'ECTS Mismatch Incomplete Certificate', 'Course subject ECTS total is different from the course ECTS', 'completion',
     'template-ects-mismatch', NULL, NULL, 'draft', NULL),
    (1011, 1007, 10070, 1007, 'Hundred Point Certificate', 'Certificate final grade uses a 0 to 100 scale', 'qualification',
     'template-ects-100', 'CERT-FULL-1011', '2026-06-01 09:00:00', 'issued', 87.00);

INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (1000, 1000),
    (1000, 1001),
    (1000, 1002),
    (1000, 1003),
    (1000, 1004),
    (1000, 1005),
    (1000, 1006),
    (1000, 1007),
    (1000, 1008),
    (1000, 1009),
    (1002, 1010),
    (1002, 1011),
    (1003, 1010),
    (1003, 1011),
    (1005, 1014),
    (1006, 1014),
    (1007, 1014),
    (1011, 1017);

INSERT INTO activity_log (
    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
    affected_entity_identifier, occurred_at, outcome, source_ip
) VALUES
    (236, 8, 101, 'LOGIN', 'user_session', '101', '2026-01-11 09:00:00', 'success', '127.0.0.1'),
    (237, 4, 102, 'LOGOUT', 'user_session', '102', '2026-01-11 10:45:00', 'success', '127.0.0.1'),
    (238, 12, 103, 'LOGIN', 'user_session', '103', '2026-01-11 11:00:00', 'success', '127.0.0.1'),
    (239, 1, 100, 'CREATE', 'organization', '12', '2026-01-03 10:00:00', 'success', '127.0.0.1'),
    (240, 8, NULL, 'UPDATE', 'organization', '10', '2026-01-04 10:00:00', 'success', '127.0.0.1'),
    (241, 9, NULL, 'DENY', 'organization', '10', '2026-01-04 10:05:00', 'failure', '127.0.0.1'),
    (242, 10, NULL, 'CREATE', 'course', '32', '2026-02-01 10:00:00', 'success', '127.0.0.1'),
    (243, 14, NULL, 'CREATE', 'subject', '42', '2026-02-01 10:10:00', 'success', '127.0.0.1'),
    (244, 11, NULL, 'ENROLL', 'class_group', '53', '2026-03-01 10:00:00', 'success', '127.0.0.1'),
    (245, 3, NULL, 'UPLOAD', 'content_item', '74', '2026-03-02 09:00:00', 'success', '127.0.0.1'),
    (246, 3, NULL, 'SCHEDULE', 'lesson', '82', '2026-03-04 10:00:00', 'success', '127.0.0.1'),
    (247, 3, NULL, 'PUBLISH', 'assessment', '94', '2026-03-08 10:00:00', 'success', '127.0.0.1'),
    (248, 15, 104, 'SUBMIT', 'attempt', '122', '2026-03-10 10:40:00', 'success', '127.0.0.1'),
    (249, 3, NULL, 'CORRECT', 'grade_record', '182', '2026-06-30 12:10:00', 'success', '127.0.0.1'),
    (250, 1, NULL, 'ISSUE', 'certificate', '195', '2026-05-15 10:00:00', 'success', '127.0.0.1'),
    (251, 12, NULL, 'SEND', 'message', '229', '2026-04-07 10:02:00', 'success', '127.0.0.1'),
    (252, 15, 104, 'READ', 'message', '225', '2026-03-04 18:20:00', 'success', '127.0.0.1'),
    (253, 1, 100, 'APPROVE', 'absence_justification', '161', '2026-03-07 10:00:00', 'success', '127.0.0.1'),
    (254, 8, NULL, 'REJECT', 'absence_justification', '162', '2026-04-12 10:00:00', 'success', '127.0.0.1'),
    (255, 1, 100, 'AUDIT', 'activity_log', '247', '2026-06-01 09:00:00', 'success', '127.0.0.1'),
    (256, 13, NULL, 'LOGIN', 'user_account', '13', '2026-01-15 09:00:00', 'failure', '127.0.0.1'),
    (257, 10, NULL, 'VIEW', 'management_view', '202', '2026-06-01 09:10:00', 'success', '127.0.0.1'),
    (258, 16, NULL, 'MANAGE', 'organization', '12', '2026-06-01 09:20:00', 'success', '127.0.0.1');

-- Exhaustive full-mode coverage pack.
-- The 3000+ id range is isolated from the main demo flows and covers rare states,
-- enum values and cross-module combinations for manual QA and regression testing.

INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (3000, 'Coverage Course Admin', 'coverage.admin.course@gape.local', 'active', 'pt-PT', 'users/3000/profile.webp',
     '2026-06-01 08:00:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-COV-3000'),
    (3001, 'Coverage Coordinator', 'coverage.coord@gape.local', 'active', 'en-US', 'users/3001/profile.webp',
     '2026-06-01 08:05:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'PASSPORT', 'PASS-COV-3001'),
    (3002, 'Coverage Teacher', 'coverage.teacher@gape.local', 'active', 'pt-PT', 'users/3002/profile.webp',
     '2026-06-01 08:10:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', NULL, NULL),
    (3003, 'Coverage Student Alpha', 'coverage.student.alpha@gape.local', 'active', 'pt-PT', 'users/3003/profile.webp',
     '2026-06-01 08:15:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'RESIDENCE_PERMIT', 'RP-COV-3003'),
    (3004, 'Coverage Student Beta', 'coverage.student.beta@gape.local', 'active', 'pt-PT', 'users/3004/profile.webp',
     '2026-06-01 08:20:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-COV-3004'),
    (3005, 'Coverage Student Gamma', 'coverage.student.gamma@gape.local', 'active', 'en-US', 'users/3005/profile.webp',
     '2026-06-01 08:25:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', NULL, NULL),
    (3006, 'Coverage Student Withdrawn', 'coverage.student.withdrawn@gape.local', 'active', 'pt-PT', 'users/3006/profile.webp',
     '2026-06-01 08:30:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'PASSPORT', 'PASS-COV-3006'),
    (3007, 'Coverage Student Delta', 'coverage.student.delta@gape.local', 'active', 'pt-PT', 'users/3007/profile.webp',
     '2026-06-01 08:35:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', NULL, NULL),
    (3008, 'Coverage Student Inactive', 'coverage.student.inactive@gape.local', 'inactive', 'pt-PT', 'users/3008/profile.webp',
     '2026-06-01 08:40:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', NULL, NULL),
    (3009, 'Coverage Student Blocked', 'coverage.student.blocked@gape.local', 'blocked', 'pt-PT', 'users/3009/profile.webp',
     '2026-06-01 08:45:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', NULL, NULL);

INSERT INTO administrator_profile (id_user, cod_administrator) VALUES
    (3000, 'ADM-COV-3000');

INSERT INTO coordinator_profile (id_user, cod_coordinator) VALUES
    (3001, 'COO-COV-3001');

INSERT INTO teacher_profile (id_user, cod_teacher) VALUES
    (3002, 'TCH-COV-3002');

INSERT INTO student_profile (id_user, cod_student) VALUES
    (3003, 'STD-COV-3003'),
    (3004, 'STD-COV-3004'),
    (3005, 'STD-COV-3005'),
    (3006, 'STD-COV-3006'),
    (3007, 'STD-COV-3007'),
    (3008, 'STD-COV-3008'),
    (3009, 'STD-COV-3009');

INSERT INTO user_session (
    id_session, id_user, token, state, start_at, last_activity, end_at
) VALUES
    (3000, 3000, 'tok-coverage-active-3000', 'active', '2026-06-02 09:00:00', '2026-06-02 09:15:00', NULL),
    (3001, 3001, 'tok-coverage-expired-3001', 'expired', '2026-06-02 10:00:00', '2026-06-02 10:20:00', '2026-06-02 10:30:00'),
    (3002, 3002, 'tok-coverage-closed-3002', 'closed', '2026-06-02 11:00:00', '2026-06-02 11:20:00', '2026-06-02 11:25:00');

INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id) VALUES
    (3000, 'MANAGE_LEARNING', 'COURSE', 30),
    (3000, 'MANAGE_LEARNING', 'SUBJECT', 40),
    (3000, 'MANAGE_LEARNING', 'CLASS_GROUP', 50);

INSERT INTO grant_coordinator (id_coordinator_user, cod_permission) VALUES
    (3001, 'MANAGE_LEARNING'),
    (3001, 'VIEW_REPORTS');

INSERT INTO grant_teacher (id_teacher_user, cod_permission) VALUES
    (3002, 'MANAGE_LEARNING'),
    (3002, 'VIEW_REPORTS');

INSERT INTO grant_student (id_student_user, cod_permission) VALUES
    (3003, 'VIEW_REPORTS'),
    (3004, 'VIEW_REPORTS'),
    (3005, 'VIEW_REPORTS'),
    (3006, 'VIEW_REPORTS'),
    (3007, 'VIEW_REPORTS');

INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (3000, 10, 'SCH-COV', 'Coverage School', 'SCHCOV', 'school', 'active', NULL),
    (3001, 10, 'FAC-COV', 'Coverage Faculty', 'FACCOV', 'faculty', 'active', 3000),
    (3002, 10, 'CTR-COV', 'Coverage Research Center', 'CTRCOV', 'center', 'active', 3001),
    (3003, 10, 'OTH-COV', 'Coverage Other Unit', 'OTHCOV', 'other', 'inactive', 3000);

INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, photo, description, ects, duration, frequency, type, state
) VALUES
    (3000, 10, 3001, 'Full Coverage Programme', 'FCOV', NULL,
     'Programme dedicated to exhaustive full seed coverage.', 30.00, '1', 'semester', 'other', 'active'),
    (3007, 10, 3001, 'Full Coverage Trimester Programme', 'FCOVT', NULL,
     'Programme dedicated to trimester value coverage.', 16.00, '1', 'trimester', 'other', 'active');

INSERT INTO course_period_template (
    id_course, curricular_year, term, starts_month, starts_day, ends_month, ends_day
) VALUES
    (3000, 1, 'semester_1', 1, 1, 6, 30),
    (3000, 1, 'semester_2', 7, 1, 12, 31),
    (3007, 1, 'trimester_1', 1, 1, 3, 31),
    (3007, 1, 'trimester_2', 4, 1, 6, 30),
    (3007, 1, 'trimester_3', 7, 1, 9, 30),
    (3007, 1, 'trimester_4', 10, 1, 12, 31);

INSERT INTO course_occurrence (
    id_course_occurrence, id_course, reference_year, label, starts_at, ends_at, state
) VALUES
    (30000, 3000, 2026, '2026', '2026-01-01', '2026-12-31', 'active'),
    (30001, 3000, 2027, '2027', '2027-01-01', '2027-12-31', 'scheduled'),
    (30070, 3007, 2026, '2026', '2026-01-01', '2026-12-31', 'active');

INSERT INTO course_occurrence_period (
    id_course_occurrence_period, id_course_occurrence, curricular_year, term, starts_at, ends_at, state
) VALUES
    (300001, 30000, 1, 'semester_1', '2026-01-01', '2026-06-30', 'active'),
    (300002, 30000, 1, 'semester_2', '2026-07-01', '2026-12-31', 'active'),
    (300011, 30001, 1, 'semester_1', '2027-01-01', '2027-06-30', 'scheduled'),
    (300012, 30001, 1, 'semester_2', '2027-07-01', '2027-12-31', 'scheduled'),
    (300071, 30070, 1, 'trimester_1', '2026-01-01', '2026-03-31', 'active'),
    (300072, 30070, 1, 'trimester_2', '2026-04-01', '2026-06-30', 'active'),
    (300073, 30070, 1, 'trimester_3', '2026-07-01', '2026-09-30', 'active'),
    (300074, 30070, 1, 'trimester_4', '2026-10-01', '2026-12-31', 'active');

INSERT INTO subject (
    id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
) VALUES
    (3000, 10, 'Coverage Annual Subject', 'FCOV-A', NULL, 'Annual subject coverage.', 6.00, 60, 'active'),
    (3001, 10, 'Coverage Semester One Subject', 'FCOV-S1', NULL, 'Semester one coverage.', 6.00, 60, 'active'),
    (3002, 10, 'Coverage Semester Two Subject', 'FCOV-S2', NULL, 'Semester two coverage.', 6.00, 60, 'active'),
    (3003, 10, 'Coverage Trimester One Subject', 'FCOV-T1', NULL, 'Trimester one coverage.', 4.00, 40, 'active'),
    (3004, 10, 'Coverage Trimester Two Subject', 'FCOV-T2', NULL, 'Trimester two coverage.', 4.00, 40, 'active'),
    (3005, 10, 'Coverage Trimester Three Subject', 'FCOV-T3', NULL, 'Trimester three coverage.', 4.00, 40, 'active');

INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory
) VALUES
    (3000, 3000, 1, 'semester_1', 1),
    (3000, 3001, 1, 'semester_1', 1),
    (3000, 3002, 1, 'semester_2', 1),
    (3000, 3003, 1, 'semester_1', 0),
    (3000, 3004, 1, 'semester_2', 0),
    (3000, 3005, 1, 'semester_2', 0),
    (3007, 3003, 1, 'trimester_1', 0),
    (3007, 3004, 1, 'trimester_2', 0),
    (3007, 3005, 1, 'trimester_3', 0),
    (3007, 3001, 1, 'trimester_4', 0);

INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state) VALUES
    (3001, 3002, 'active');

INSERT INTO class_group (
    id_class_group, id_subject, id_course, id_course_occurrence, id_course_occurrence_period, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (3000, 3000, 3000, 30001, 300011, 'FCOV-DRAFT', 'onsite', 'draft', 1, 20, '2027-01-01', '2027-06-30', 'morning'),
    (3001, 3001, 3000, 30001, 300011, 'FCOV-DRAFT-ONLINE', 'online', 'draft', 1, 20, '2027-01-01', '2027-06-30', 'afternoon'),
    (3002, 3002, 3000, 30000, 300002, 'FCOV-ACTIVE', 'hybrid', 'active', 1, 30, '2026-07-01', '2026-12-31', 'mixed'),
    (3003, 3003, 3000, 30000, 300001, 'FCOV-COMPLETED', 'onsite', 'completed', 1, 20, '2026-01-01', '2026-06-30', 'evening'),
    (3004, 3004, 3000, 30000, 300002, 'FCOV-ONLINE', 'online', 'active', 1, 20, '2026-07-01', '2026-12-31', 'morning'),
    (3005, 3005, 3000, 30000, 300002, 'FCOV-HYBRID', 'hybrid', 'active', 1, 20, '2026-07-01', '2026-12-31', 'afternoon'),
    -- A genuine upcoming cohort: it is intentionally empty before enrollment
    -- opens, while its occurrence and period are both scheduled.
    (3006, 3001, 3000, 30001, 300011, 'FCOV-SCHEDULED', 'hybrid', 'scheduled', 8, 24, '2027-01-01', '2027-06-30', 'evening');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (3002, 3000, 'inactive', '2026-06-01', NULL),
    (3002, 3001, 'inactive', '2026-06-01', NULL),
    (3002, 3002, 'active', '2026-06-01', NULL),
    (3002, 3003, 'inactive', '2026-01-01', '2026-03-01'),
    (3002, 3004, 'active', '2026-06-01', NULL),
    (3002, 3005, 'inactive', '2026-06-01', NULL);

INSERT INTO class_group_enrollment_policy (id_class_group, approval_mode) VALUES
    (3001, 'auto_approve'),
    (3002, 'manual'),
    (3004, 'auto_approve');

INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date) VALUES
    (3003, 3000, 30000, 'active', '2026-07-01', '2026-12-31'),
    (3004, 3000, 30000, 'completed', '2026-01-01', '2026-06-30'),
    (3005, 3000, 30000, 'active', '2026-07-01', '2026-12-31'),
    (3006, 3000, 30000, 'withdrawn', '2026-06-01', '2026-06-10'),
    (3007, 3000, 30000, 'active', '2026-07-01', '2026-12-31'),
    (3008, 3000, 30000, 'inactive', '2026-06-01', NULL);

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (3003, 3002, 'active', '2026-07-01', NULL),
    (3005, 3002, 'active', '2026-07-01', NULL),
    (3007, 3002, 'active', '2026-07-01', NULL),
    (3004, 3001, 'pending', '2027-01-01', NULL),
    (3005, 3004, 'inactive', '2026-07-01', NULL),
    (3006, 3003, 'withdrawn', '2026-01-01', '2026-02-01'),
    (3007, 3004, 'rejected', '2026-07-01', '2026-07-02'),
    (3004, 3003, 'completed', '2026-01-01', '2026-03-01');

INSERT INTO content_item (
    id_content_item, author_user_id, title, description, format, source, state, created_at, updated_at
) VALUES
    (3000, 3002, 'Coverage Text', 'Text content coverage.', 'text', 'contents/coverage/text.html', 'active', '2026-06-02 09:00:00', NULL),
    (3001, 3002, 'Coverage Image', 'Image content coverage.', 'image', 'contents/coverage/image.webp', 'active', '2026-06-02 09:05:00', NULL),
    (3002, 3002, 'Coverage Video', 'Video content coverage.', 'video', 'contents/coverage/video.mp4', 'active', '2026-06-02 09:10:00', NULL),
    (3003, 3002, 'Coverage Audio', 'Audio content coverage.', 'audio', 'contents/coverage/audio.m4a', 'active', '2026-06-02 09:15:00', NULL),
    (3004, 3002, 'Coverage PDF', 'PDF content coverage.', 'pdf', 'contents/coverage/document.pdf', 'active', '2026-06-02 09:20:00', NULL),
    (3005, 3002, 'Coverage Archive', 'Archive content coverage.', 'archive', 'contents/coverage/archive.zip', 'active', '2026-06-02 09:25:00', NULL),
    (3006, 3002, 'Coverage URL', 'External URL coverage.', 'url', 'https://example.org/coverage', 'active', '2026-06-02 09:30:00', NULL),
    (3007, 3002, 'Coverage SCORM', 'SCORM package coverage.', 'scorm', 'contents/coverage/scorm.zip', 'active', '2026-06-02 09:35:00', NULL),
    (3008, 3002, 'Coverage xAPI', 'xAPI package coverage.', 'xapi', 'contents/coverage/xapi.zip', 'active', '2026-06-02 09:40:00', NULL),
    (3009, 3002, 'Coverage Presentation', 'Presentation coverage.', 'presentation', 'contents/coverage/slides.pdf', 'active', '2026-06-02 09:45:00', NULL),
    (3010, 3002, 'Coverage Embed Draft', 'Embed draft coverage.', 'embed', '<iframe title="Coverage"></iframe>', 'draft', '2026-06-02 09:50:00', NULL),
    (3011, 3002, 'Coverage Other Inactive', 'Other inactive coverage.', 'other', 'contents/coverage/other.bin', 'inactive', '2026-06-02 09:55:00', '2026-06-02 10:00:00');

INSERT INTO content_file (
    id_content_file, id_content_item, original_filename, original_mime_type, final_mime_type,
    original_bytes, final_bytes, sha256, original_path, final_path, thumbnail_path,
    duration_seconds, width, height, page_count, processing_state, processing_error,
    created_at, processed_at
) VALUES
    (3100, 3001, 'coverage-image.png', 'image/png', 'image/webp',
     303, 5060, 'f61334cdc7583c02dfd8131e98ee8386d8592eae172e976f9c55e3b7cc8279fb', 'uploads/originals/coverage-image.png', 'contents/coverage/image.webp', 'contents/coverage/thumb-image.webp',
     NULL, 200, 200, NULL, 'ready', NULL, '2026-06-02 09:05:00', '2026-06-02 09:06:00'),
    (3101, 3002, 'coverage-video.mp4', 'video/mp4', 'video/mp4',
     42655, 42655, 'a1a8410841a09cc131ee75836134f019598e6b754dc7ad090e11a513a77b8d99', 'uploads/originals/coverage-video.mp4', 'contents/coverage/video.mp4', NULL,
     3, 1280, 720, NULL, 'processing', NULL, '2026-06-02 09:10:00', NULL),
    (3102, 3004, 'coverage-document.pdf', 'application/pdf', 'application/pdf',
     1016, 1016, '57c74788436687d77a34a3378a7fee925a6f60dc76cf20f10c49146a9e067ba1', 'uploads/originals/coverage-document.pdf', 'contents/coverage/document.pdf', NULL,
     NULL, NULL, NULL, 1, 'failed', 'OCR normalization failed in demo coverage.', '2026-06-02 09:20:00', '2026-06-02 09:22:00');

INSERT INTO associate_organization_content (id_organization, id_content_item, role) VALUES
    (10, 3000, 'catalog');

INSERT INTO associate_organic_unit_content (id_organic_unit, id_content_item, role) VALUES
    (3001, 3001, 'showcase');

INSERT INTO associate_course_content (id_course, id_content_item, role) VALUES
    (3000, 3004, 'syllabus');

INSERT INTO associate_subject_content (id_subject, id_content_item, role) VALUES
    (3002, 3006, 'reference');

INSERT INTO associate_class_group_content (id_class_group, id_content_item, role) VALUES
    (3002, 3002, 'support');

INSERT INTO physical_room (
    cod_physical_room, id_organization, id_organic_unit, name, description, capacity, location, state
) VALUES
    ('COV-ACTIVE', 10, 3001, 'Coverage Active Room', 'Room for active coverage scenarios.', 35, 'Coverage Building A', 'active'),
    ('COV-EXAM', 10, 3001, 'Coverage Exam Room', 'Room reserved for assessment coverage.', 35, 'Coverage Building D', 'active'),
    ('COV-INACTIVE', 10, 3001, 'Coverage Inactive Room', 'Inactive room coverage.', 20, 'Coverage Building B', 'inactive'),
    ('COV-UNAVAILABLE', 10, 3001, 'Coverage Unavailable Room', 'Unavailable room coverage.', 15, 'Coverage Building C', 'unavailable');

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    state
) VALUES
    (3000, 3002, 'FCOV-OPEN', 'Coverage Open Block', 'Open active block coverage.', 1, 'active'),
    (3001, 3002, 'FCOV-RESTRICTED', 'Coverage Restricted Block', 'Restricted active block coverage.', 2, 'active'),
    (3002, 3002, 'FCOV-RESTRICTED-2', 'Coverage Restricted Block 2', 'Additional restricted block coverage.', 3, 'active'),
    (3003, 3002, 'FCOV-OPEN-2', 'Coverage Open Block 2', 'Additional open block coverage.', 4, 'active'),
    (3004, 3002, 'FCOV-RESTRICTED-3', 'Coverage Restricted Block 3', 'Extra restricted block coverage.', 5, 'active');

INSERT INTO associate_block_content (id_content_block, id_content_item, order_no, role, mandatory) VALUES
    (3000, 3000, 1, 'main', 1),
    (3000, 3003, 2, 'support', 0),
    (3001, 3004, 1, 'statement', 1),
    (3002, 3009, 1, 'slides', 1);

INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type,
    provider, access_url, attendance_required, state, starts_at, ends_at, order_no
) VALUES
    (3000, 3002, 3000, 'COV-ACTIVE', 'Coverage Draft Hybrid Lesson', 'Draft lesson coverage.', 'hybrid',
     'Teams', 'https://teams.microsoft.com/l/meetup-join/coverage-draft', 0, 'draft',
     '2026-10-04 09:00:00', '2026-10-04 11:00:00', 1),
    (3001, 3002, 3000, 'COV-ACTIVE', 'Coverage Scheduled Hybrid Lesson', 'Scheduled lesson coverage.', 'hybrid',
     'Teams', 'https://teams.microsoft.com/l/meetup-join/coverage-scheduled', 1, 'scheduled',
     '2026-10-05 09:00:00', '2026-10-05 11:00:00', 2),
    (3002, 3002, 3000, NULL, 'Coverage Active Online Lesson', 'Active lesson coverage.', 'online',
     'Zoom', 'https://zoom.example/coverage-active', 1, 'active',
     '2026-07-02 00:00:00', '2026-12-20 23:59:59', 3),
    (3003, 3002, 3001, 'COV-ACTIVE', 'Coverage Completed Onsite Lesson', 'Completed lesson coverage.', 'onsite',
     NULL, NULL, 1, 'completed', '2026-07-03 09:00:00', '2026-07-03 11:00:00', 4),
    (3004, 3002, 3001, 'COV-ACTIVE', 'Coverage Cancelled Onsite Lesson', 'Cancelled lesson coverage.', 'onsite',
     NULL, NULL, 0, 'cancelled', '2026-10-06 09:00:00', '2026-10-06 11:00:00', 5);

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
    max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode, state, available_from, available_until, order_no
) VALUES
    (3000, 3002, 3000, NULL, 'Coverage Active Form', 'Active automatic form with every question type.', 'form', 'online', 'automatic',
     20.00, 10.00, 20.00, 5, 'auto_approve', 'active', '2026-07-02 00:00:00', '2026-12-20 23:59:59', 1),
    (3001, 3002, 3000, NULL, 'Coverage Scheduled Test', 'Scheduled mixed test coverage.', 'test', 'online', 'mixed',
     20.00, 10.00, 20.00, 2, 'manual', 'scheduled', '2026-10-10 09:00:00', '2026-10-10 11:00:00', 2),
    (3002, 3002, NULL, 'COV-ACTIVE', 'Coverage Completed Exam', 'Completed subject-level onsite exam.', 'exam', 'onsite', 'manual',
     20.00, 10.00, 20.00, 1, 'auto_approve', 'completed', '2026-07-02 09:00:00', '2026-07-02 11:00:00', 3),
    (3003, 3002, 3001, NULL, 'Coverage Draft Form', 'Draft assessment coverage.', 'form', 'online', 'automatic',
     20.00, 10.00, 20.00, 1, 'manual', 'draft', '2026-08-01 09:00:00', '2026-08-01 11:00:00', 4),
    (3004, 3002, NULL, 'COV-EXAM', 'Coverage Active Subject Exam', 'Active subject-level exam coverage.', 'exam', 'onsite', 'manual',
     20.00, 10.00, 20.00, 2, 'manual', 'active', '2026-07-02 00:00:00', '2026-12-20 23:59:59', 5);

INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (3002, 3002),
    (3004, 3002);

INSERT INTO associate_assessment_content (id_assessment, id_content_item, role) VALUES
    (3000, 3004, 'statement'),
    (3001, 3009, 'support'),
    (3004, 3005, 'attachment');

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer
) VALUES
    (3000, 3000, 'FCOV-Q1', 'Choose the single correct option.', 'single_choice', 1, 1, 2.00, NULL),
    (3001, 3000, 'FCOV-Q2', 'Choose every correct option.', 'multiple_choice', 2, 1, 3.00, NULL),
    (3002, 3000, 'FCOV-Q3', 'Write a short answer.', 'short_text', 3, 1, 3.00, 'short coverage answer'),
    (3003, 3000, 'FCOV-Q4', 'Write a paragraph answer.', 'paragraph', 4, 0, 4.00, 'manual rubric'),
    (3004, 3000, 'FCOV-Q5', 'Upload the requested file.', 'file_upload', 5, 1, 4.00, NULL),
    (3005, 3000, 'FCOV-Q6', 'Rate the coverage scenario.', 'rating', 6, 1, 4.00,
     'rating_style=stars;rating_step=integer;rating_max=5;expected_value=4');

INSERT INTO question_option (id_option, id_question, order_no, text, correct_flag) VALUES
    (3000, 3000, 1, 'Single correct option', 1),
    (3001, 3000, 2, 'Single wrong option', 0),
    (3002, 3001, 1, 'First correct option', 1),
    (3003, 3001, 2, 'Second correct option', 1),
    (3004, 3001, 3, 'Distractor option', 0);

INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (3003, 3000, 'active'),
    (3005, 3000, 'active'),
    (3007, 3000, 'active'),
    (3004, 3000, 'pending'),
    (3006, 3000, 'withdrawn'),
    (3004, 3001, 'inactive'),
    (3005, 3001, 'rejected'),
    (3007, 3001, 'completed'),
    (3003, 3004, 'active');

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (3000, 3003, 3000, 1, NULL, 'in_progress', '2026-07-02 09:05:00', NULL),
    (3001, 3003, 3000, 2, NULL, 'submitted', '2026-07-02 09:10:00', '2026-07-02 09:50:00'),
    (3002, 3003, 3000, 3, 16.00, 'corrected', '2026-07-02 10:00:00', '2026-07-02 10:45:00'),
    (3003, 3003, 3000, 4, NULL, 'expired', '2026-07-02 11:00:00', NULL),
    (3004, 3003, 3000, 5, NULL, 'cancelled', '2026-07-02 11:30:00', NULL),
    (3005, 3005, 3000, 1, 12.00, 'corrected', '2026-07-02 09:00:00', '2026-07-02 10:00:00'),
    (3006, 3007, 3000, 1, 14.00, 'corrected', '2026-07-02 09:00:00', '2026-07-02 10:10:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (3000, 3002, 3000, 'FCOV-R1', NULL, NULL, 2.00, '2026-07-02 10:10:00'),
    (3001, 3002, 3001, 'FCOV-R2', NULL, NULL, 3.00, '2026-07-02 10:12:00'),
    (3002, 3002, 3002, 'FCOV-R3', 'short coverage answer', NULL, 3.00, '2026-07-02 10:15:00'),
    (3003, 3002, 3003, 'FCOV-R4', 'A complete paragraph answer for manual correction.', NULL, 3.00, '2026-07-02 10:20:00'),
    (3004, 3002, 3004, 'FCOV-R5', NULL, 'attempts/3002/upload.pdf', 3.00, '2026-07-02 10:25:00'),
    (3005, 3002, 3005, 'FCOV-R6', '5', NULL, 2.00, '2026-07-02 10:30:00');

INSERT INTO response_option (id_response, id_option) VALUES
    (3000, 3000),
    (3001, 3002),
    (3001, 3003);

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (3000, 3001, NULL, 'Coverage Scheduled Lesson Event', 'Linked lesson event coverage.', 'lesson',
     '2026-10-05 09:00:00', '2026-10-05 11:00:00', 0, 1, 30, 'active'),
    (3001, NULL, 3001, 'Coverage Scheduled Assessment Event', 'Linked assessment event coverage.', 'assessment',
     '2026-10-10 09:00:00', '2026-10-10 11:00:00', 0, 1, 60, 'active'),
    (3002, NULL, NULL, 'Coverage Draft Reminder', 'Draft reminder coverage.', 'reminder',
     '2026-10-01 08:00:00', '2026-10-01 08:15:00', 0, 0, NULL, 'draft'),
    (3003, NULL, NULL, 'Coverage Inactive Meeting', 'Inactive meeting coverage.', 'meeting',
     '2026-10-02 14:00:00', '2026-10-02 15:00:00', 0, 0, NULL, 'inactive'),
    (3004, NULL, NULL, 'Coverage Cancelled Other Event', 'Cancelled event coverage.', 'other',
     '2026-10-03 09:00:00', '2026-10-03 10:00:00', 0, 1, 10, 'cancelled'),
    (3005, NULL, NULL, 'Coverage Completed Other Event', 'Completed event coverage.', 'other',
     '2026-06-01 09:00:00', '2026-06-01 10:00:00', 1, 0, NULL, 'completed');

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
    (3001, 3000),
    (3002, 3000),
    (3003, 3000),
    (3005, 3000),
    (3007, 3000),
    (3001, 3001),
    (3002, 3001),
    (3003, 3001),
    (3005, 3001),
    (3007, 3001),
    (3001, 3002),
    (3002, 3002),
    (3003, 3002),
    (3005, 3002),
    (3007, 3002),
    (3001, 3003),
    (3002, 3003),
    (3003, 3003),
    (3005, 3003),
    (3007, 3003),
    (3002, 3004),
    (3002, 3005);

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (3000, 3002),
    (3001, 3002),
    (3002, 3002),
    (3003, 3002),
    (3004, 3004),
    (3005, 3005);

INSERT INTO attendance_record (
    id_attendance_record, id_lesson, id_user_student, status, source, check_in, check_out, notes, state
) VALUES
    (3000, 3002, 3003, 'present', 'manual', '2026-07-02 09:00:00', '2026-07-02 11:00:00', 'Present full session.', 'active'),
    (3001, 3002, 3005, 'absent', 'automatic', NULL, NULL, 'Auto absence for justification request.', 'active'),
    (3002, 3002, 3007, 'late', 'other', '2026-07-02 09:35:00', '2026-07-02 11:00:00', 'Late arrival.', 'active'),
    (3003, 3003, 3003, 'partial', 'manual', '2026-06-10 09:00:00', '2026-06-10 10:00:00', 'Partial permanence.', 'active'),
    (3004, 3003, 3005, 'absent', 'manual', NULL, NULL, 'Approved absence to become justified.', 'active'),
    (3005, 3003, 3007, 'late', 'automatic', '2026-06-10 09:20:00', '2026-06-10 11:00:00', 'Cancelled justification case.', 'active'),
    (3006, 3000, 3003, 'absent', 'other', NULL, NULL, 'Cancelled duplicate attendance coverage.', 'cancelled');

INSERT INTO absence_justification (
    id_absence_justification, id_attendance_record, id_user_student_submitter, id_user_processor,
    submitted_at, reason, attachment, processed_at, decision_notes, state
) VALUES
    (3000, 3001, 3005, NULL, '2026-07-02 12:00:00', 'Medical appointment request.', 'justifications/3000.pdf', NULL, NULL, 'submitted'),
    (3001, 3002, 3007, NULL, '2026-07-02 12:10:00', 'Transport incident under review.', NULL, NULL, NULL, 'under_review'),
    (3002, 3004, 3005, 3002, '2026-06-10 12:00:00', 'Approved medical absence.', 'justifications/3002.pdf', '2026-06-11 09:00:00', 'Accepted with document.', 'approved'),
    (3003, 3003, 3003, 3002, '2026-06-10 12:15:00', 'Rejected partial attendance claim.', NULL, '2026-06-11 09:10:00', 'Evidence insufficient.', 'rejected'),
    (3004, 3005, 3007, NULL, '2026-06-10 12:30:00', 'Cancelled by student.', NULL, NULL, NULL, 'cancelled');

UPDATE attendance_record
SET status = 'justified',
    state = 'corrected',
    notes = 'Absence justified by approved request.'
WHERE id_attendance_record = 3004;

INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, id_course_occurrence, title, type, max_grade, passing_grade, weight_alert, released_at, state
) VALUES
    (3000, 3002, 30000, 'Coverage Draft Partial Grade Sheet', 'partial', 20.00, 10.00, NULL, NULL, 'draft'),
    (3001, 3002, 30000, 'Coverage Inactive Other Grade Sheet', 'other', 20.00, 10.00, NULL, '2026-06-30 12:00:00', 'inactive'),
    (3002, 3002, 30000, 'Coverage Closed Final Grade Sheet', 'final', 20.00, 10.00, NULL, '2026-07-02 12:00:00', 'closed');

INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (3000, 3002, 100.00),
    (3001, 3002, 100.00),
    (3002, 3000, 100.00);

INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (3000, 3002),
    (3001, 3002),
    (3002, 3002);

INSERT INTO grade_record (
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, state, recorded_at, notes
) VALUES
    (3000, 3000, 3003, NULL, 'AUTO-3000-3003', 16.00, 'approved', 'draft', '2026-07-02 12:10:00', 'Draft record coverage.'),
    (3001, 3000, 3005, NULL, 'AUTO-3000-3005', 7.00, 'failed', 'corrected', '2026-07-02 12:12:00', 'Corrected failed record coverage.'),
    (3002, 3000, 3007, NULL, 'AUTO-3000-3007', 0.00, 'absent', 'published', '2026-07-02 12:14:00', 'Published absent record coverage.'),
    (3003, 3001, 3003, NULL, 'HIST-3001-3003', 0.00, 'pending', 'inactive', '2026-06-30 12:20:00', 'Inactive pending record coverage.'),
    (3004, 3002, 3003, 3002, 'AUTO-3002-3003', 16.00, 'approved', 'published', '2026-07-02 12:30:00', 'Closed sheet record.'),
    (3005, 3002, 3005, 3005, 'AUTO-3002-3005', 12.00, 'approved', 'published', '2026-07-02 12:31:00', 'Closed sheet record.'),
    (3006, 3002, 3007, 3006, 'AUTO-3002-3007', 14.00, 'approved', 'published', '2026-07-02 12:32:00', 'Closed sheet record.');

INSERT INTO certificate (
    id_certificate, id_course, id_course_occurrence, id_user_student, title, notes, type, template, validation_code, issued_at, state, final_grade
) VALUES
    (3000, 3000, 30000, 3003, 'Coverage Completion Certificate', 'Draft completion certificate coverage with incomplete course grades.', 'completion', 'template-coverage-completion', NULL, NULL, 'draft', NULL),
    (3001, 3000, 30000, 3004, 'Coverage Attendance Certificate', 'Draft attendance certificate coverage.', 'attendance', 'template-coverage-attendance', NULL, NULL, 'draft', NULL),
    (3002, 3000, 30000, 3005, 'Coverage Qualification Certificate', 'Draft qualification certificate coverage.', 'qualification', 'template-coverage-qualification', NULL, NULL, 'draft', NULL),
    (3003, 3000, 30000, 3006, 'Coverage Other Certificate', 'Draft other certificate coverage for an ineligible student.', 'other', 'template-coverage-other', NULL, NULL, 'draft', NULL);

INSERT INTO management_view (
    id_management_view, title, type, description, visibility_scope, state
) VALUES
    (3000, 'Coverage Global Dashboard', 'dashboard', 'Global coverage dashboard.', 'GLOBAL', 'active'),
    (3001, 'Coverage Private Report', 'report', 'Private coverage report.', 'USER', 'inactive'),
    (3002, 'Coverage Inactive Analytics', 'analytics', 'Inactive analytics coverage.', 'COURSE', 'inactive');

INSERT INTO access_management_view (id_user, id_management_view) VALUES
    (3000, 3000),
    (3001, 3000),
    (3002, 3001),
    (3003, 3002);

INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (3000, 'Coverage Direct Messages', 'message', 'public', '2026-07-02 08:00:00', 'active'),
    (3001, 'Coverage Forum', 'forum', 'context', '2026-07-02 08:05:00', 'active'),
    (3002, 'Coverage Comments', 'comments', 'private', '2026-07-02 08:10:00', 'active'),
    (3003, 'Coverage Announcements', 'announcement', 'organization', '2026-07-02 08:15:00', 'active'),
    (3004, 'Coverage System Channel', 'system', 'system', '2026-07-02 08:20:00', 'active'),
    (3005, 'Coverage Organization Channel', 'organization', 'organization', '2026-07-02 08:25:00', 'active'),
    (3006, 'Coverage Class Group Channel', 'class_group', 'participants', '2026-07-02 08:30:00', 'active'),
    (3007, 'Coverage Content Block Channel', 'content_block', 'context', '2026-07-02 08:35:00', 'active'),
    (3008, 'Coverage Assessment Channel', 'assessment', 'participants', '2026-07-02 08:40:00', 'active'),
    (3009, 'Coverage Inactive Channel', 'other', 'private', '2026-07-02 08:45:00', 'inactive');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (3002, 3000, 'owner', '2026-07-02 08:00:00', 0, 'active'),
    (3003, 3000, 'member', '2026-07-02 08:01:00', 0, 'active'),
    (3004, 3000, 'viewer', '2026-07-02 08:02:00', 1, 'inactive'),
    (3005, 3000, 'moderator', '2026-07-02 08:03:00', 0, 'blocked'),
    (3001, 3001, 'coordinator', '2026-07-02 08:05:00', 0, 'active'),
    (3002, 3001, 'teacher', '2026-07-02 08:06:00', 0, 'active'),
    (3003, 3001, 'student', '2026-07-02 08:07:00', 0, 'active'),
    (3002, 3002, 'owner', '2026-07-02 08:10:00', 0, 'active'),
    (3000, 3003, 'administrator', '2026-07-02 08:15:00', 0, 'active'),
    (3000, 3004, 'administrator', '2026-07-02 08:20:00', 0, 'active'),
    (3003, 3004, 'student', '2026-07-02 08:21:00', 0, 'active'),
    (3000, 3005, 'owner', '2026-07-02 08:25:00', 0, 'active'),
    (3002, 3006, 'teacher', '2026-07-02 08:30:00', 0, 'active'),
    (3003, 3006, 'student', '2026-07-02 08:31:00', 0, 'active'),
    (3005, 3006, 'student', '2026-07-02 08:32:00', 0, 'active'),
    (3002, 3007, 'teacher', '2026-07-02 08:35:00', 0, 'active'),
    (3002, 3008, 'teacher', '2026-07-02 08:40:00', 0, 'active');

INSERT INTO associate_channel_class_group (id_channel, id_class_group) VALUES
    (3001, 3002),
    (3006, 3002),
    (3007, 3002),
    (3008, 3002);

INSERT INTO associate_channel_content_block (id_channel, id_content_block) VALUES
    (3007, 3000);

INSERT INTO associate_channel_assessment (id_channel, id_assessment) VALUES
    (3008, 3000);

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (3000, 3000, 3002, NULL, NULL, 'Coverage Active Text', 'Active text message coverage.', 'text',
     'normal', NULL, '2026-07-02 09:00:00', NULL, NULL, '2026-07-02 09:00:30', 'active'),
    (3001, 3000, 3003, 3000, NULL, 'Coverage Edited Comment', 'Edited reply coverage.', 'comment',
     'low', NULL, '2026-07-02 09:05:00', '2026-07-02 09:10:00', NULL, '2026-07-02 09:05:30', 'edited'),
    (3002, 3004, NULL, NULL, NULL, 'Coverage Alert', 'System alert coverage.', 'alert',
     'urgent', NULL, '2026-07-02 09:10:00', NULL, NULL, '2026-07-02 09:10:05', 'sent'),
    (3003, 3003, 3000, NULL, NULL, 'Coverage Cancelled Announcement', 'Cancelled announcement coverage.', 'announcement',
     'high', NULL, '2026-07-02 09:15:00', NULL, NULL, NULL, 'cancelled'),
    (3004, 3006, 3002, NULL, NULL, 'Coverage Attachment', 'Attachment message coverage.', 'attachment',
     'high', 'messages/coverage/attachment.pdf', '2026-07-02 09:20:00', NULL, NULL, '2026-07-02 09:20:30', 'sent'),
    (3005, 3001, 3002, NULL, NULL, 'Coverage Warning', 'Warning message coverage.', 'warning',
     'urgent', NULL, '2026-07-02 09:25:00', NULL, NULL, '2026-07-02 09:25:10', 'active'),
    (3006, 3001, 3001, NULL, 3001, 'Coverage Scheduled Reminder', 'Scheduled reminder coverage.', 'reminder',
     'normal', NULL, '2026-07-02 09:30:00', NULL, '2026-10-10 08:00:00', NULL, 'scheduled'),
    (3007, 3004, NULL, NULL, NULL, 'Coverage Notification', 'Notification message coverage.', 'notification',
     'normal', NULL, '2026-07-02 09:35:00', NULL, NULL, '2026-07-02 09:35:05', 'sent'),
    (3008, 3004, NULL, NULL, NULL, 'Coverage System Message', 'System message coverage.', 'system',
     'normal', NULL, '2026-07-02 09:40:00', NULL, NULL, '2026-07-02 09:40:05', 'sent'),
    (3009, 3001, 3001, NULL, NULL, 'Coverage Deleted Other', 'Deleted other message coverage.', 'other',
     'low', NULL, '2026-07-02 09:45:00', '2026-07-02 09:50:00', NULL, '2026-07-02 09:45:10', 'deleted');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, state
) VALUES
    (3003, 3000, NULL, NULL, 'pending'),
    (3003, 3001, '2026-07-02 09:06:00', NULL, 'delivered'),
    (3002, 3001, '2026-07-02 09:06:00', '2026-07-02 09:20:00', 'read'),
    (3003, 3002, NULL, NULL, 'pending'),
    (3005, 3004, '2026-07-02 09:21:00', NULL, 'delivered'),
    (3003, 3006, NULL, NULL, 'pending'),
    (3000, 3007, '2026-07-02 09:36:00', '2026-07-02 09:45:00', 'read');

INSERT INTO activity_log (
    id_activity_log, id_user, id_session, operation_type, affected_entity_type,
    affected_entity_identifier, occurred_at, outcome, source_ip
) VALUES
    (3000, 3000, 3000, 'BOOTSTRAP_FULL_COVERAGE', 'seed', 'full-coverage-pack', '2026-07-02 14:00:00', 'success', '127.0.0.1'),
    (3001, 3002, 3002, 'CREATE', 'assessment', '3000', '2026-07-02 14:05:00', 'success', '127.0.0.1'),
    (3002, 3002, NULL, 'UPDATE', 'attendance_record', '3004', '2026-07-02 14:10:00', 'success', '127.0.0.1'),
    (3003, 3001, 3001, 'MODERATE', 'message', '3001', '2026-07-02 14:15:00', 'success', '127.0.0.1'),
    (3004, 3007, NULL, 'MESSAGE_DELIVERY_FAILURE', 'message', '3002', '2026-07-02 14:20:00', 'failure', '127.0.0.1');

-- RC-MATH-05 is the focused, current Mathematics cohort used in the class-group
-- detail demonstrations.  It deliberately has a complete teaching plan, active
-- enrolments, pending requests and submitted answers that still need correction.
INSERT INTO user_account (
    id_user, name, email, state, language, photo, created_at, credential_hash, credential_salt, document_type, document_number
) VALUES
    (6500, 'Mathematics Request Student One', 'rc.math.request1@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:00:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6500'),
    (6501, 'Mathematics Request Student Two', 'rc.math.request2@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:05:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6501'),
    (6502, 'Mathematics Request Student Three', 'rc.math.request3@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:10:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6502'),
    (6503, 'Mathematics Request Student Four', 'rc.math.request4@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:15:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6503'),
    (6504, 'Mathematics Request Student Five', 'rc.math.request5@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:20:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6504'),
    (6505, 'Mathematics Student One', 'rc.math.student1@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:25:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6505'),
    (6506, 'Mathematics Student Two', 'rc.math.student2@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:30:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6506'),
    (6507, 'Mathematics Student Three', 'rc.math.student3@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:35:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6507'),
    (6508, 'Mathematics Student Four', 'rc.math.student4@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:40:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6508'),
    (6509, 'Mathematics Student Five', 'rc.math.student5@gape.local', 'active', 'pt-PT', NULL, '2026-03-01 08:45:00', 'ZsAsa7ClmLV+Ai2LaLAJdrW030r/BuQJ98CexaRn1Ss=', '8scgIe5H/ymYYNE9mx/Zzw==', 'CITIZEN_CARD', 'CC-RCM-6509');

INSERT INTO student_profile (id_user, cod_student) VALUES
    (6500, 'STD-RCM-6500'),
    (6501, 'STD-RCM-6501'),
    (6502, 'STD-RCM-6502'),
    (6503, 'STD-RCM-6503'),
    (6504, 'STD-RCM-6504'),
    (6505, 'STD-RCM-6505'),
    (6506, 'STD-RCM-6506'),
    (6507, 'STD-RCM-6507'),
    (6508, 'STD-RCM-6508'),
    (6509, 'STD-RCM-6509');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (3, 65, 'active', NULL, NULL),
    (6, 65, 'active', NULL, NULL);

INSERT INTO class_group_enrollment_policy (id_class_group, approval_mode) VALUES
    (65, 'manual');

INSERT INTO enroll_course (id_student_user, id_course, id_course_occurrence, state, start_date, end_date) VALUES
    (6500, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6501, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6502, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6503, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6504, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6505, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6506, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6507, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6508, 34, 345, 'active', '2026-03-01', '2026-08-01'),
    (6509, 34, 345, 'active', '2026-03-01', '2026-08-01');

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (6505, 65, 'active', '2026-03-02', NULL),
    (6506, 65, 'active', '2026-03-02', NULL),
    (6507, 65, 'active', '2026-03-02', NULL),
    (6508, 65, 'active', '2026-03-02', NULL),
    (6509, 65, 'active', '2026-03-02', NULL),
    (6500, 65, 'pending', '2026-07-01', NULL),
    (6501, 65, 'pending', '2026-07-02', NULL),
    (6502, 65, 'pending', '2026-07-03', NULL),
    (6503, 65, 'pending', '2026-07-04', NULL),
    (6504, 65, 'pending', '2026-07-05', NULL);

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no, state
) VALUES
    (6500, 65, 'RCM-01', 'Functions and Graphs', 'Model linear and quadratic functions from practical data.', 1, 'active'),
    (6501, 65, 'RCM-02', 'Algebraic Reasoning', 'Solve equations and explain each transformation.', 2, 'active'),
    (6502, 65, 'RCM-03', 'Sequences and Change', 'Use sequences, limits and rates of change in context.', 3, 'active'),
    (6503, 65, 'RCM-04', 'Probability and Data', 'Interpret distributions and justify probabilistic conclusions.', 4, 'active'),
    (6504, 65, 'RCM-05', 'Mathematical Modelling', 'Combine the course topics in a defensible mathematical model.', 5, 'active');

INSERT INTO lesson (
    id_lesson, id_class_group, id_content_block, cod_physical_room, title, description, type, provider,
    access_url, attendance_required, state, starts_at, ends_at, order_no
) VALUES
    (6500, 65, 6500, 'SALA-EX02', 'Function Workshop', 'Guided modelling workshop with graph interpretation.', 'hybrid', 'Teams', 'https://teams.microsoft.com/l/meetup-join/rc-math-05-functions', 1, 'completed', '2026-07-03 09:00:00', '2026-07-03 11:00:00', 1),
    (6501, 65, 6501, NULL, 'Equation Clinic', 'Live online problem-solving session for algebraic methods.', 'online', 'Teams', 'https://teams.microsoft.com/l/meetup-join/rc-math-05-algebra', 1, 'completed', '2026-07-08 09:00:00', '2026-07-08 11:00:00', 1),
    (6502, 65, 6502, 'SALA-EX02', 'Sequences Laboratory', 'Onsite exploration of sequences and change.', 'onsite', NULL, NULL, 1, 'completed', '2026-07-10 09:00:00', '2026-07-10 11:00:00', 1),
    (6503, 65, 6503, 'SALA-EX02', 'Data Interpretation Studio', 'Hybrid session on probability distributions and evidence.', 'hybrid', 'Teams', 'https://teams.microsoft.com/l/meetup-join/rc-math-05-data', 1, 'completed', '2026-07-13 09:00:00', '2026-07-13 11:00:00', 1),
    (6504, 65, 6504, 'SALA-EX02', 'Modelling Review', 'Capstone review of the mathematical modelling project.', 'hybrid', 'Teams', 'https://teams.microsoft.com/l/meetup-join/rc-math-05-modelling', 1, 'completed', '2026-07-14 09:00:00', '2026-07-14 11:00:00', 1);

INSERT INTO assessment (
    id_assessment, id_subject, id_content_block, cod_physical_room, title, description, type, mode, correction_mode,
    max_grade, passing_grade, final_grade_weight, attempts_limit, enrollment_mode, state, available_from, available_until, order_no
) VALUES
    (6500, 43, 6500, NULL, 'Functions Applied Checkpoint', 'Explain a function model and interpret its graph.', 'test', 'online', 'manual', 20.00, 9.50, 20.00, 1, 'auto_approve', 'completed', '2026-07-03 11:15:00', '2026-07-04 23:59:59', 1),
    (6501, 43, 6501, NULL, 'Algebraic Reasoning Checkpoint', 'Show the intermediate steps for a system of equations.', 'test', 'online', 'manual', 20.00, 9.50, 20.00, 1, 'auto_approve', 'completed', '2026-07-08 11:15:00', '2026-07-09 23:59:59', 1),
    (6502, 43, 6502, NULL, 'Sequences Investigation', 'Justify the behaviour of a sequence in a real scenario.', 'test', 'online', 'manual', 20.00, 9.50, 20.00, 1, 'auto_approve', 'completed', '2026-07-10 11:15:00', '2026-07-11 23:59:59', 1),
    (6503, 43, 6503, NULL, 'Probability Interpretation', 'Analyse a distribution and support a conclusion with data.', 'test', 'online', 'manual', 20.00, 9.50, 20.00, 1, 'auto_approve', 'completed', '2026-07-13 11:15:00', '2026-07-14 23:59:59', 1),
    (6504, 43, 6504, 'SALA-EX02', 'Mathematical Modelling Portfolio', 'Present and defend the final mathematical model.', 'exam', 'onsite', 'manual', 20.00, 9.50, 20.00, 1, 'auto_approve', 'active', '2026-07-15 13:00:00', '2026-07-18 17:00:00', 1);

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer
) VALUES
    (6500, 6500, 'RCM-F-01', 'Explain how the domain affects the interpretation of your function model.', 'paragraph', 1, 1, 20.00, 'The answer must connect the admissible input values to the real situation.'),
    (6501, 6501, 'RCM-A-01', 'Solve the system and explain the algebraic operations used.', 'paragraph', 1, 1, 20.00, 'A complete solution with justified elimination or substitution steps.'),
    (6502, 6502, 'RCM-S-01', 'Describe the long-term behaviour of the sequence and justify it.', 'paragraph', 1, 1, 20.00, 'A justified conclusion based on the general term or recurrence.'),
    (6503, 6503, 'RCM-P-01', 'Interpret the observed distribution and state a supported conclusion.', 'paragraph', 1, 1, 20.00, 'The conclusion must refer to centre, spread and the contextual implication.'),
    (6504, 6504, 'RCM-M-01', 'Present the assumptions, calculations and validation of your model.', 'paragraph', 1, 1, 20.00, 'A complete model must state assumptions, show calculations and validate the result.');

INSERT INTO enroll_assessment (id_student_user, id_assessment, state) VALUES
    (6505, 6500, 'active'), (6506, 6501, 'active'), (6507, 6502, 'active'), (6508, 6503, 'active'), (6509, 6504, 'active');

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (6500, 6505, 6500, 1, NULL, 'submitted', '2026-07-03 12:00:00', '2026-07-03 12:28:00'),
    (6501, 6506, 6501, 1, NULL, 'submitted', '2026-07-08 12:00:00', '2026-07-08 12:33:00'),
    (6502, 6507, 6502, 1, NULL, 'submitted', '2026-07-10 12:00:00', '2026-07-10 12:24:00'),
    (6503, 6508, 6503, 1, NULL, 'submitted', '2026-07-13 12:00:00', '2026-07-13 12:37:00'),
    (6504, 6509, 6504, 1, NULL, 'submitted', '2026-07-15 13:30:00', '2026-07-15 14:20:00');

INSERT INTO response (
    id_response, id_attempt, id_question, cod_response, answer, attachment, score, answered_at
) VALUES
    (6500, 6500, 6500, 'RCM-R-01', 'The number of units cannot be negative, so the model is interpreted only for non-negative production values.', NULL, NULL, '2026-07-03 12:28:00'),
    (6501, 6501, 6501, 'RCM-R-02', 'I eliminated y by subtracting the equations, solved for x, then substituted x back to obtain y.', NULL, NULL, '2026-07-08 12:33:00'),
    (6502, 6502, 6502, 'RCM-R-03', 'The sequence approaches a stable value because the successive differences become smaller and tend to zero.', NULL, NULL, '2026-07-10 12:24:00'),
    (6503, 6503, 6503, 'RCM-R-04', 'Most observations cluster near the centre, but the upper tail shows a small group with substantially higher values.', NULL, NULL, '2026-07-13 12:37:00'),
    (6504, 6504, 6504, 'RCM-R-05', 'I assumed linear demand, calculated the break-even point and checked the model against the observed data.', NULL, NULL, '2026-07-15 14:20:00');

INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, id_course_occurrence, title, type, max_grade, passing_grade, weight_alert, released_at, state
) VALUES
    (6500, 43, 345, 'RC-MATH-05 Continuous Assessment', 'continuous_assessment', 20.00, 9.50,
     'Five submitted answers are awaiting manual correction.', NULL, 'draft');

INSERT INTO associate_grade_sheet_class_group (id_grade_sheet, id_class_group) VALUES
    (6500, 65);

INSERT INTO based_on_assessment (id_grade_sheet, id_assessment, weight) VALUES
    (6500, 6500, 20.00), (6500, 6501, 20.00), (6500, 6502, 20.00),
    (6500, 6503, 20.00), (6500, 6504, 20.00);

COMMIT;
