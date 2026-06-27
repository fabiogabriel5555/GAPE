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
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (51, 40, 30, 'PRJ-PL2', 'online', 'active', 5, 35, '2026-02-01', '2026-06-30', 'morning');

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (61, 51, 'BLK-02', 'Planning', 'Second course unit block', 1, 'restricted', 'active',
     '2026-02-15 00:00:00', '2026-04-15 23:59:59');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (6, 51, 'active', '2026-02-01', NULL);

INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date) VALUES
    (7, 30, 'active', '2026-02-01', NULL);

INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date) VALUES
    (7, 30, 40, 'active', '2026-02-01', NULL);

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
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, enrollment_mode, state, available_from, available_until
) VALUES
    (93, 40, 61, 'PRJ-PL2 Online Form', 'Demo online class group assessment', 'form', 'online', 'automatic',
     20.00, 9.50, 2, 'auto_approve', 'active', '2026-02-17 00:00:00', '2026-02-20 23:59:59');

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer, state
) VALUES
    (102, 93, 'Q1', 'What is the goal of sprint planning?', 'single_choice', 1, 1, 20.00, NULL, 'active');

INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag, state
) VALUES
    (114, 102, 1, 'Define and plan the sprint work', 1, 'active'),
    (115, 102, 2, 'Close the current sprint', 0, 'active');

INSERT INTO enroll_assessment (id_student_user, id_assessment, state, start_date, end_date) VALUES
    (7, 93, 'active', '2026-02-17', NULL);

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (142, 81, NULL, 'Evento Online Lesson 1', 'PRJ-PL2 online class group session', 'lesson',
     '2026-02-18 09:00:00', '2026-02-18 11:00:00', 0, 1, 20, 'active');

INSERT INTO receive_schedule_event (id_user, id_schedule_event) VALUES
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
    (181, 170, 7, 121, 'GR-002', 8.00, 'reproved', '2026-02-12 12:05:00', 'Needs study reinforcement', 'active');

-- Demo messages in the channel
INSERT INTO channel (
    id_channel, title, type, visibility, created_at, state
) VALUES
    (210, 'PRJ-PL2 Online Channel', 'class_group', 'participants', '2026-02-15 09:55:00', 'active');

INSERT INTO participate_channel (id_user, id_channel, role, joined_at, muted, state) VALUES
    (6, 210, 'teacher', '2026-02-15 10:00:00', 0, 'active'),
    (7, 210, 'student', '2026-02-15 10:00:00', 0, 'active');

INSERT INTO message (
    id_message, id_channel, id_user_sender, id_parent_message, id_schedule_event_origin,
    title, body, type, priority, attachment, created_at, updated_at, scheduled_at, sent_at, state
) VALUES
    (224, 210, 6, NULL, 142, 'Online Lesson Preparation', 'Review the material before the session', 'announcement',
     'high', NULL, '2026-02-17 18:00:00', NULL, NULL, '2026-02-17 18:01:00', 'sent');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, delivery_mode, state
) VALUES
    (7, 224, '2026-02-17 18:01:10', '2026-02-17 18:30:00', 'internal', 'read');

-- Additional certificate
INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template,
    validation_code, issued_at, final_grade, state
) VALUES
    (193, 30, 7, 'Participation Certificate', 'Participation in the module', 'attendance', 'template-v1',
     'VAL-2026-0002', '2026-07-01 11:00:00', 8.00, 'issued');

INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (193, 170);

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
    (1, 13, 'inactive', '2026-01-01', '2026-03-31');

UPDATE organization
SET state = 'active'
WHERE id_organization = 12;

INSERT INTO organic_unit (
    id_organic_unit, id_organization, cod_organic_unit, name, acronym, type, state, parent_organic_unit_id
) VALUES
    (23, 10, 'QA', 'Academic Quality Office', 'GQA', 'office', 'active', 20),
    (24, 12, 'FOR', 'Training Department', 'DFOR', 'direction', 'active', NULL),
    (25, 10, 'LEG', 'Historical Unit', 'UH', 'service', 'inactive', NULL);

INSERT INTO course (
    id_course, id_organization, id_organic_unit, name, acronym, photo, description, ects, duration, type, state
) VALUES
    (32, 10, 22, 'Information Systems Master', 'MSI', NULL, 'Second-cycle course for full coverage', 120.00, '2', 'master', 'active'),
    (33, 12, 24, 'Systems Quality and Audit', 'QAS', NULL, 'Professional quality training', 20.00, '1', 'professional_training', 'active'),
    (34, 10, 25, 'Archived Legacy Course', 'LEG', NULL, 'Course kept for historical records only', 30.00, '1', 'other', 'inactive');

INSERT INTO subject (
    id_subject, id_organization, name, acronym, photo, description, ects, workload_hours, state
) VALUES
    (42, 10, 'Databases', 'BD', NULL, 'Relational modeling and persistence', 6.00, 70, 'active'),
    (43, 10, 'Computer Networks', 'RC', NULL, 'Network and service fundamentals', 6.00, 65, 'active'),
    (44, 12, 'Industrial Safety', 'SI', NULL, 'Safety subject in a business context', 4.00, 30, 'active'),
    (45, 13, 'Archived Content', 'ARQ', NULL, 'Historical subject for archived organization', 3.00, 20, 'inactive');

INSERT INTO integrate_subject (
    id_course, id_subject, curricular_year, term, mandatory, state
) VALUES
    (30, 42, 2, 'semester_2', 1, 'active'),
    (32, 42, 1, 'semester_1', 1, 'active'),
    (32, 43, 1, 'semester_2', 0, 'active'),
    (33, 44, 1, 'trimester_1', 1, 'active'),
    (34, 42, 1, 'annual', 0, 'inactive');

INSERT INTO coordinate_subject (id_coordinator_user, id_subject, state, start_date, end_date) VALUES
    (14, 42, 'active', '2026-02-01', NULL),
    (12, 43, 'active', '2026-02-01', NULL),
    (2, 41, 'inactive', '2025-09-01', '2026-01-31');

INSERT INTO class_group (
    id_class_group, id_subject, id_course, cod_class_group, modality, state,
    min_students, max_students, starts_at, ends_at, shift
) VALUES
    (53, 42, 30, 'BD-T1', 'hybrid', 'active', 8, 35, '2026-03-01', '2026-06-30', 'evening'),
    (54, 43, 32, 'RC-T1', 'onsite', 'completed', 6, 25, '2026-03-01', '2026-05-31', 'morning'),
    (55, 44, 33, 'SI-T1', 'online', 'active', 4, 20, '2026-04-01', '2026-05-31', 'afternoon'),
    (56, 42, 32, 'BD-ARCH', 'online', 'completed', 5, 20, '2025-02-01', '2025-06-30', 'mixed');

INSERT INTO content_block (
    id_content_block, id_class_group, cod_content_block, name, description, order_no,
    access_mode, state, available_from, available_until
) VALUES
    (63, 53, 'BD-01', 'Relational Model', 'Block about data modeling', 1, 'scheduled', 'active',
     '2026-03-01 00:00:00', '2026-03-31 23:59:59'),
    (64, 53, 'BD-02', 'Applied SQL', 'Block in preparation', 2, 'restricted', 'draft',
     '2026-04-01 00:00:00', NULL),
    (65, 55, 'SI-01', 'Safety Standards', 'Initial business training block', 1, 'open', 'active',
     '2026-04-01 00:00:00', '2026-05-01 23:59:59'),
    (66, 54, 'RC-LEG', 'Closed Laboratory', 'Historical networks block', 1, 'restricted', 'inactive',
     '2026-03-01 00:00:00', '2026-05-31 23:59:59');

INSERT INTO teach_class_group (id_teacher_user, id_class_group, state, start_date, end_date) VALUES
    (3, 53, 'active', '2026-03-01', NULL),
    (6, 53, 'inactive', '2026-03-01', '2026-03-31'),
    (12, 55, 'active', '2026-04-01', NULL);

INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date) VALUES
    (4, 32, 'active', '2026-03-01', NULL),
    (7, 32, 'withdrawn', '2026-03-01', '2026-03-20'),
    (12, 33, 'active', '2026-04-01', NULL),
    (15, 30, 'active', '2026-03-01', NULL),
    (15, 32, 'active', '2026-03-01', NULL),
    (15, 33, 'active', '2026-04-01', NULL);

INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date) VALUES
    (4, 32, 42, 'withdrawn', '2026-03-01', '2026-03-01'),
    (4, 30, 42, 'active', '2026-03-01', NULL),
    (12, 33, 44, 'active', '2026-04-01', NULL),
    (15, 30, 40, 'active', '2026-03-01', NULL),
    (15, 30, 42, 'active', '2026-03-01', NULL),
    (15, 32, 42, 'withdrawn', '2026-03-01', '2026-03-01'),
    (15, 32, 43, 'active', '2026-03-01', NULL),
    (15, 33, 44, 'active', '2026-04-01', NULL);

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
    ('SALA-B1', 10, 22, 'Sala B1', 'Sala de bases de dados', 35, 'Edificio B', 'active'),
    ('SALA-HYB', 10, 20, 'Hybrid Room', 'Room equipped for hybrid classes', 30, 'Building A', 'active'),
    ('SALA-C1', 12, 24, 'Sala C1', 'Sala de formacao empresarial', 18, 'Campus Empresa', 'active'),
    ('SALA-INAT', 12, 24, 'Sala Inativa', 'Espaco temporariamente indisponivel', 10, 'Campus Empresa', 'inactive');

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
    id_assessment, id_subject, id_content_block, title, description, type, mode, correction_mode,
    max_grade, passing_grade, attempts_limit, enrollment_mode, state, available_from, available_until
) VALUES
    (94, 42, 63, 'Databases Practical Exam', 'Practical databases assessment', 'exam', 'onsite', 'manual',
     20.00, 9.50, 2, 'manual', 'active', '2026-03-10 09:00:00', '2026-03-10 11:00:00'),
    (95, 42, 64, 'SQL Draft Form', 'Form in preparation', 'form', 'online', 'automatic',
     20.00, 10.00, 3, 'auto_approve', 'draft', '2026-04-01 00:00:00', '2026-04-15 23:59:59'),
    (96, 44, 65, 'Safety Form', 'Online industrial safety assessment', 'form', 'online', 'automatic',
     20.00, 10.00, 1, 'auto_approve', 'active', '2026-04-09 00:00:00', '2026-04-12 23:59:59'),
    (97, 43, NULL, 'Networks Exam', 'Networks exam without pedagogical block', 'exam', 'onsite', 'manual',
     20.00, 9.50, 1, 'manual', 'completed', '2026-05-20 09:00:00', '2026-05-20 11:00:00'),
    (98, 42, 63, 'Complete Databases Assessment', 'Demo assessment with all question types', 'test', 'online', 'mixed',
     20.00, 10.00, 3, 'auto_approve', 'active', '2026-03-18 08:00:00', '2026-03-28 23:59:59');

INSERT INTO assessment_class_group (id_assessment, id_class_group) VALUES
    (97, 54);

INSERT INTO associate_assessment_content (id_assessment, id_content_item, role) VALUES
    (94, 74, 'statement'),
    (95, 76, 'support'),
    (96, 77, 'statement');

INSERT INTO question (
    id_question, id_assessment, cod_question, statement, type, order_no, required_flag, score, expected_answer, state
) VALUES
    (103, 94, 'BD-Q1', 'Select the applicable normal forms.', 'multiple_choice', 1, 1, 8.00, NULL, 'active'),
    (104, 94, 'BD-Q2', 'Explain the difference between a primary key and a foreign key.', 'paragraph', 2, 1, 8.00,
     'Free response assessed manually', 'active'),
    (105, 94, 'BD-Q3', 'Attach the final SQL script.', 'file_upload', 3, 1, 4.00, NULL, 'active'),
    (106, 95, 'SQL-Q1', 'Choose the query command.', 'single_choice', 1, 1, 20.00, NULL, 'active'),
    (107, 96, 'SI-Q1', 'Rate the displayed risk.', 'rating', 1, 1, 20.00, '4', 'active'),
    (109, 97, 'RC-Q1', 'How many usable hosts does a /24 mask provide?', 'short_text', 1, 1, 20.00, '254', 'inactive'),
    (110, 98, 'FULL-Q1', 'Choose the SQL statement used to query data.', 'single_choice', 1, 1, 2.00, NULL, 'active'),
    (111, 98, 'FULL-Q2', 'Select all properties that belong to an ACID transaction.', 'multiple_choice', 2, 1, 3.00, NULL, 'active'),
    (112, 98, 'FULL-Q3', 'Enter the keyword used to remove rows from a table.', 'short_text', 3, 1, 2.00, 'DELETE', 'active'),
    (113, 98, 'FULL-Q4', 'Explain when an index should be created on a column.', 'paragraph', 4, 1, 4.00,
     'Free response automatically assessed as textual reference.', 'active'),
    (114, 98, 'FULL-Q5', 'Attach the final entity-relationship diagram.', 'file_upload', 5, 1, 3.00, 'formats=pdf,image,archive', 'active'),
    (116, 98, 'FULL-Q6', 'Rate the clarity of the prompt.', 'rating', 6, 1, 6.00,
     'rating_style=stars;rating_step=half;rating_max=5;expected_value=4.5', 'active');

INSERT INTO question_option (
    id_option, id_question, order_no, text, correct_flag, state
) VALUES
    (116, 103, 1, 'First normal form', 1, 'active'),
    (117, 103, 2, 'Second normal form', 1, 'active'),
    (118, 103, 3, 'Non-normalized form', 0, 'active'),
    (119, 106, 1, 'SELECT', 1, 'active'),
    (120, 106, 2, 'DROP', 0, 'active'),
    (121, 107, 1, '1', 0, 'active'),
    (122, 107, 2, '4', 1, 'active'),
    (123, 107, 3, '5', 0, 'inactive'),
    (124, 110, 1, 'SELECT', 1, 'active'),
    (125, 110, 2, 'UPDATE', 0, 'active'),
    (126, 110, 3, 'CREATE INDEX', 0, 'active'),
    (127, 111, 1, 'Atomicity', 1, 'active'),
    (128, 111, 2, 'Consistency', 1, 'active'),
    (129, 111, 3, 'Durability', 1, 'active'),
    (130, 111, 4, 'Rendering', 0, 'active');

INSERT INTO enroll_assessment (id_student_user, id_assessment, state, start_date, end_date) VALUES
    (4, 94, 'pending', '2026-03-10', NULL),
    (15, 94, 'active', '2026-03-10', NULL),
    (15, 95, 'active', '2026-04-01', NULL),
    (12, 96, 'active', '2026-04-09', NULL),
    (15, 97, 'active', '2026-05-20', NULL),
    (4, 98, 'active', '2026-03-18', NULL),
    (15, 98, 'active', '2026-03-18', NULL);

INSERT INTO attempt (
    id_attempt, id_student_user, id_assessment, attempt_number, score, state, started_at, submitted_at
) VALUES
    (122, 15, 94, 1, 16.00, 'corrected', '2026-03-10 09:05:00', '2026-03-10 10:40:00'),
    (123, 15, 95, 1, NULL, 'in_progress', '2026-04-03 10:00:00', NULL),
    (124, 12, 96, 1, 15.00, 'submitted', '2026-04-09 14:00:00', '2026-04-09 14:25:00'),
    (125, 15, 94, 2, NULL, 'expired', '2026-03-10 10:45:00', NULL),
    (126, 15, 97, 1, 12.00, 'submitted', '2026-05-20 09:00:00', '2026-05-20 10:15:00'),
    (127, 15, 98, 1, 14.50, 'submitted', '2026-03-18 09:00:00', '2026-03-18 09:55:00'),
    (128, 15, 98, 2, NULL, 'in_progress', '2026-03-19 10:00:00', NULL),
    (129, 4, 98, 1, NULL, 'cancelled', '2026-03-18 11:00:00', NULL);

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
    (145, 127, 116, 'FULL-R6', '4.5', NULL, 3.00, '2026-03-18 09:50:00');

INSERT INTO response_option (id_response, id_option) VALUES
    (132, 116),
    (132, 117),
    (135, 119),
    (136, 122),
    (139, 124),
    (140, 127),
    (140, 128),
    (140, 129);

INSERT INTO schedule_event (
    id_schedule_event, id_lesson, id_assessment, title, description, type, starts_at, ends_at,
    all_day, reminder_enabled, reminder_minutes_before, state
) VALUES
    (143, 82, NULL, 'Databases Hybrid Lesson Event', 'Relational model session', 'lesson',
     '2026-03-05 18:00:00', '2026-03-05 20:00:00', 0, 1, 30, 'active'),
    (144, 83, NULL, 'SQL Laboratory Event', 'In-person SQL session', 'lesson',
     '2026-04-10 18:00:00', '2026-04-10 20:00:00', 0, 1, 60, 'draft'),
    (145, 84, NULL, 'Evento Online Safety Lesson', 'Formacao online', 'lesson',
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
    (15, 143),
    (3, 147),
    (15, 147),
    (12, 145),
    (12, 148),
    (15, 145),
    (4, 151),
    (15, 151),
    (1, 150),
    (14, 150);

INSERT INTO associate_schedule_event_class_group (id_schedule_event, id_class_group) VALUES
    (143, 53),
    (144, 53),
    (145, 55),
    (146, 54),
    (147, 53),
    (148, 55),
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

INSERT INTO grade_sheet (
    id_grade_sheet, id_subject, title, type, released_at, state
) VALUES
    (171, 42, 'Databases Final Grade Sheet', 'final', '2026-06-30 12:00:00', 'published'),
    (172, 42, 'Databases Exam Grade Sheet', 'exam', '2026-03-15 12:00:00', 'closed'),
    (173, 44, 'Industrial Safety Grade Sheet', 'continuous_assessment', NULL, 'draft'),
    (174, 43, 'Archived Networks Grade Sheet', 'other', '2026-05-30 12:00:00', 'closed');

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
    id_grade_record, id_grade_sheet, id_user_student, id_attempt, cod_grade_record, value, result, recorded_at, notes, state
) VALUES
    (182, 171, 15, 122, 'BD-FINAL-015', 16.00, 'approved', '2026-06-30 12:10:00', 'Approved with good performance', 'published'),
    (183, 171, 4, NULL, 'BD-FINAL-004', 0.00, 'absent', '2026-06-30 12:15:00', 'No final submission', 'draft'),
    (184, 172, 15, 125, 'BD-EXAM-015', 0.00, 'failed', '2026-03-15 12:10:00', 'Expired attempt', 'corrected'),
    (185, 173, 12, 124, 'SI-CA-012', 15.00, 'approved', '2026-04-15 12:00:00', 'Assessment submitted', 'published'),
    (186, 174, 15, 126, 'RC-ARCH-015', 12.00, 'approved', '2026-05-30 12:00:00', 'Historical record', 'corrected'),
    (187, 171, 7, NULL, 'BD-FINAL-007', 9.00, 'pending', '2026-06-30 12:20:00', 'Awaiting review', 'active');

INSERT INTO certificate (
    id_certificate, id_course, id_user_student, title, notes, type, template,
    validation_code, issued_at, final_grade, state
) VALUES
    (194, 30, 15, 'Active Databases Certificate', 'Certificate ready to issue', 'completion', 'template-v2',
     NULL, NULL, 16.00, 'active'),
    (195, 33, 12, 'Safety Qualification Certificate', 'Business qualification completed', 'qualification', 'template-company',
     'VAL-2026-0003', '2026-05-15 10:00:00', 15.00, 'issued'),
    (196, 30, 15, 'Revoked Databases Certificate', 'Revoked due to replacement', 'completion', 'template-v2',
     'VAL-2026-0004', '2026-07-05 10:00:00', 16.00, 'revoked'),
    (197, 32, 15, 'Archived Networks Certificate', 'Archived document', 'other', 'template-archive',
     NULL, NULL, 12.00, 'revoked');

INSERT INTO based_on_grade_sheet_certificate (id_certificate, id_grade_sheet) VALUES
    (194, 171),
    (195, 173),
    (196, 171),
    (197, 174);

INSERT INTO management_view (
    id_management_view, title, type, description, visibility_scope, state
) VALUES
    (200, 'Global Administration Dashboard', 'dashboard', 'Global system indicators', 'GLOBAL', 'active'),
    (201, 'ISG Organization Dashboard', 'dashboard', 'ISG organization indicators', 'ORGANIZATION', 'active'),
    (202, 'LEI Course Dashboard', 'dashboard', 'LEI course indicators', 'COURSE', 'active'),
    (203, 'Databases Subject Dashboard', 'dashboard', 'Database indicators', 'SUBJECT', 'active'),
    (204, 'BD-T1 Class Group Dashboard', 'dashboard', 'Class group attendance indicators', 'CLASS_GROUP', 'active'),
    (205, 'Archived Dashboard', 'dashboard', 'Old view kept for historical records', 'GLOBAL', 'inactive');

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
    (233, 211, 3, NULL, NULL, 'Archived Message', 'Old channel content', 'other',
     'low', NULL, '2026-03-01 10:00:00', '2026-03-02 10:00:00', NULL, '2026-03-01 10:05:00', 'deleted');

INSERT INTO receive_message (
    id_user, id_message, delivered_at, read_at, delivery_mode, state
) VALUES
    (15, 225, '2026-03-04 18:05:20', '2026-03-04 18:20:00', 'both', 'read'),
    (4, 225, '2026-03-04 18:05:20', NULL, 'internal', 'delivered'),
    (3, 226, '2026-03-04 19:00:45', '2026-03-04 19:10:00', 'internal', 'read'),
    (15, 227, '2026-03-09 12:01:20', NULL, 'email', 'delivered'),
    (8, 228, NULL, NULL, 'internal', 'pending'),
    (15, 229, NULL, NULL, 'both', 'failed'),
    (15, 230, NULL, NULL, 'internal', 'pending'),
    (1, 232, '2026-03-05 10:00:20', NULL, 'internal', 'delivered');

-- Final coverage for added features:
-- enrollment policies, content file processing,
-- class group visual preferences and historical enrollment states.

INSERT INTO subject_enrollment_policy (id_course, id_subject, approval_mode) VALUES
    (30, 40, 'manual'),
    (30, 42, 'auto_approve'),
    (32, 43, 'manual'),
    (33, 44, 'auto_approve');

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

INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date) VALUES
    (17, 30, 'active', '2026-03-01', NULL),
    (17, 31, 'completed', '2026-01-01', '2026-02-01'),
    (17, 32, 'active', '2026-03-01', NULL),
    (17, 33, 'inactive', '2026-04-01', NULL),
    (17, 34, 'inactive', '2025-02-01', '2025-06-30');

INSERT INTO enroll_subject (id_student_user, id_course, id_subject, state, start_date, end_date) VALUES
    (17, 30, 40, 'active', '2026-03-01', NULL),
    (17, 30, 42, 'active', '2026-03-01', NULL),
    (17, 32, 43, 'pending', '2026-03-01', NULL),
    (17, 33, 44, 'rejected', '2026-04-01', '2026-04-02'),
    (17, 31, 41, 'completed', '2026-01-01', '2026-02-01'),
    (17, 32, 42, 'inactive', '2026-03-01', NULL),
    (17, 34, 42, 'inactive', '2025-02-01', '2025-06-30');

INSERT INTO enroll_class_group (id_student_user, id_class_group, state, start_date, end_date) VALUES
    (17, 53, 'pending', '2026-03-01', NULL),
    (17, 50, 'rejected', '2026-02-01', '2026-02-02'),
    (17, 51, 'completed', '2026-02-01', '2026-06-30'),
    (17, 54, 'inactive', '2026-03-01', NULL),
    (17, 56, 'inactive', '2025-02-01', '2025-06-30');

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

COMMIT;
