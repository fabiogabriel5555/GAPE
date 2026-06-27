-- GAPE - Drop completo do esquema
-- Executar na base de dados alvo antes de recriar schema.sql.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS activity_log;
DROP TABLE IF EXISTS associate_channel_assessment;
DROP TABLE IF EXISTS associate_channel_content_block;
DROP TABLE IF EXISTS associate_channel_class_group;
DROP TABLE IF EXISTS receive_message;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS participate_channel;
DROP TABLE IF EXISTS channel;
DROP TABLE IF EXISTS access_management_view;
DROP TABLE IF EXISTS management_view;
DROP TABLE IF EXISTS based_on_grade_sheet_certificate;
DROP TABLE IF EXISTS certificate;
DROP TABLE IF EXISTS grade_record;
DROP TABLE IF EXISTS based_on_assessment;
DROP TABLE IF EXISTS associate_grade_sheet_class_group;
DROP TABLE IF EXISTS grade_sheet;
DROP TABLE IF EXISTS absence_justification;
DROP TABLE IF EXISTS attendance_record;
DROP TABLE IF EXISTS associate_schedule_event_class_group;
DROP TABLE IF EXISTS receive_schedule_event;
DROP TABLE IF EXISTS schedule_event;

DROP TABLE IF EXISTS associate_assessment_content;
DROP TABLE IF EXISTS associate_block_content;
DROP TABLE IF EXISTS associate_class_group_content;
DROP TABLE IF EXISTS associate_subject_content;
DROP TABLE IF EXISTS associate_course_content;
DROP TABLE IF EXISTS associate_organic_unit_content;
DROP TABLE IF EXISTS associate_organization_content;
DROP TABLE IF EXISTS response_option;
DROP TABLE IF EXISTS response;
DROP TABLE IF EXISTS attempt;
DROP TABLE IF EXISTS enroll_assessment;
DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS assessment_class_group;
DROP TABLE IF EXISTS assessment;
DROP TABLE IF EXISTS lesson;
DROP TABLE IF EXISTS physical_room;
DROP TABLE IF EXISTS content_file;
DROP TABLE IF EXISTS content_item;

DROP TABLE IF EXISTS class_group_enrollment_policy;
DROP TABLE IF EXISTS subject_enrollment_policy;
DROP TABLE IF EXISTS enroll_class_group;
DROP TABLE IF EXISTS enroll_subject;
DROP TABLE IF EXISTS enroll_course;
DROP TABLE IF EXISTS teach_class_group;
DROP TABLE IF EXISTS coordinate_subject;
DROP TABLE IF EXISTS manage_organization;
DROP TABLE IF EXISTS integrate_subject;
DROP TABLE IF EXISTS content_block;
DROP TABLE IF EXISTS class_group;
DROP TABLE IF EXISTS subject;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS organic_unit;
DROP TABLE IF EXISTS organization;

DROP TABLE IF EXISTS grant_student;
DROP TABLE IF EXISTS grant_teacher;
DROP TABLE IF EXISTS grant_coordinator;
DROP TABLE IF EXISTS grant_administrator;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS deletion_request;
DROP TABLE IF EXISTS user_session;
DROP TABLE IF EXISTS student_profile;
DROP TABLE IF EXISTS teacher_profile;
DROP TABLE IF EXISTS coordinator_profile;
DROP TABLE IF EXISTS administrator_profile;
DROP TABLE IF EXISTS user_account;

SET FOREIGN_KEY_CHECKS = 1;
