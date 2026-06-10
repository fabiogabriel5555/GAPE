-- GAPE - Base de dados relacional (MySQL 8+)
-- Fonte principal: documento "0. GAPE - ALL - V3"
-- NOTA: as restricoes de integridade aplicacional cruzadas ficam no Service.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- =========================================================
-- 1) ACESSO, IDENTIDADE E CONTROLO
-- =========================================================

CREATE TABLE IF NOT EXISTS user_account (
    id_user BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    state VARCHAR(20) NOT NULL,
    language VARCHAR(10) NOT NULL,
    photo VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    credential_hash VARCHAR(255) NOT NULL,
    credential_salt VARCHAR(255) NOT NULL,
    document_type VARCHAR(40) NULL,
    document_number VARCHAR(40) NULL,
    PRIMARY KEY (id_user),
    UNIQUE KEY uq_user_account_email (email),
    UNIQUE KEY uq_user_account_document (document_type, document_number),
    KEY idx_user_account_state (state),
    KEY idx_user_account_language (language),
    CONSTRAINT ck_user_account_state
        CHECK (state IN ('active', 'inactive', 'blocked')),
    CONSTRAINT ck_user_account_document_pair
        CHECK (
            (document_type IS NULL AND document_number IS NULL)
            OR (document_type IS NOT NULL AND document_number IS NOT NULL)
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS administrator_profile (
    id_user BIGINT UNSIGNED NOT NULL,
    cod_administrator VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_user),
    UNIQUE KEY uq_administrator_profile_code (cod_administrator),
    CONSTRAINT fk_administrator_profile_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS coordinator_profile (
    id_user BIGINT UNSIGNED NOT NULL,
    cod_coordinator VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_user),
    UNIQUE KEY uq_coordinator_profile_code (cod_coordinator),
    CONSTRAINT fk_coordinator_profile_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS teacher_profile (
    id_user BIGINT UNSIGNED NOT NULL,
    cod_teacher VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_user),
    UNIQUE KEY uq_teacher_profile_code (cod_teacher),
    CONSTRAINT fk_teacher_profile_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS student_profile (
    id_user BIGINT UNSIGNED NOT NULL,
    cod_student VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_user),
    UNIQUE KEY uq_student_profile_code (cod_student),
    CONSTRAINT fk_student_profile_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_session (
    id_session BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_user BIGINT UNSIGNED NOT NULL,
    token VARCHAR(255) NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    last_activity DATETIME NOT NULL,
    end_at DATETIME NULL,
    PRIMARY KEY (id_session),
    UNIQUE KEY uq_user_session_token (token),
    KEY idx_user_session_user (id_user),
    KEY idx_user_session_state (state),
    CONSTRAINT fk_user_session_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_user_session_state
        CHECK (state IN ('active', 'expired', 'closed')),
    CONSTRAINT ck_user_session_last_activity
        CHECK (last_activity >= start_at),
    CONSTRAINT ck_user_session_last_activity_end
        CHECK (end_at IS NULL OR last_activity <= end_at),
    CONSTRAINT ck_user_session_end
        CHECK (end_at IS NULL OR end_at >= start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS deletion_request (
    id_deletion BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    submitter_user_id BIGINT UNSIGNED NOT NULL,
    processor_admin_user_id BIGINT UNSIGNED NULL,
    submitted_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    reason VARCHAR(300) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_deletion),
    KEY idx_deletion_submitter (submitter_user_id),
    KEY idx_deletion_processor (processor_admin_user_id),
    KEY idx_deletion_state (state),
    CONSTRAINT fk_deletion_submitter
        FOREIGN KEY (submitter_user_id) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_deletion_processor_admin
        FOREIGN KEY (processor_admin_user_id) REFERENCES administrator_profile (id_user)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_deletion_state
        CHECK (state IN ('submitted', 'under_review', 'approved', 'rejected', 'completed')),
    CONSTRAINT ck_deletion_final_state
        CHECK (state NOT IN ('approved', 'rejected', 'completed') OR processed_at IS NOT NULL),
    CONSTRAINT ck_deletion_processed_after_submitted
        CHECK (processed_at IS NULL OR processed_at >= submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS permission (
    cod_permission VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (cod_permission),
    KEY idx_permission_state (state),
    CONSTRAINT ck_permission_state
        CHECK (state IN ('active', 'inactive'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grant_administrator (
    id_admin_user BIGINT UNSIGNED NOT NULL,
    cod_permission VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_admin_user, cod_permission),
    KEY idx_grant_administrator_permission (cod_permission),
    CONSTRAINT fk_grant_administrator_admin
        FOREIGN KEY (id_admin_user) REFERENCES administrator_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_grant_administrator_permission
        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grant_coordinator (
    id_coordinator_user BIGINT UNSIGNED NOT NULL,
    cod_permission VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_coordinator_user, cod_permission),
    KEY idx_grant_coordinator_permission (cod_permission),
    CONSTRAINT fk_grant_coordinator_coordinator
        FOREIGN KEY (id_coordinator_user) REFERENCES coordinator_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_grant_coordinator_permission
        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grant_teacher (
    id_teacher_user BIGINT UNSIGNED NOT NULL,
    cod_permission VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_teacher_user, cod_permission),
    KEY idx_grant_teacher_permission (cod_permission),
    CONSTRAINT fk_grant_teacher_teacher
        FOREIGN KEY (id_teacher_user) REFERENCES teacher_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_grant_teacher_permission
        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grant_student (
    id_student_user BIGINT UNSIGNED NOT NULL,
    cod_permission VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_student_user, cod_permission),
    KEY idx_grant_student_permission (cod_permission),
    CONSTRAINT fk_grant_student_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_grant_student_permission
        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 2) ESTRUTURA ORGANIZACIONAL E FORMATIVA
-- =========================================================

CREATE TABLE IF NOT EXISTS organization (
    id_organization BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NULL,
    photo VARCHAR(255) NULL,
    type VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_organization),
    UNIQUE KEY uq_organization_name (name),
    CONSTRAINT ck_organization_type
        CHECK (type IN ('educational_institution', 'training_company', 'company', 'other')),
    CONSTRAINT ck_organization_state
        CHECK (state IN ('active', 'inactive', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS organic_unit (
    id_organic_unit BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_organization BIGINT UNSIGNED NOT NULL,
    cod_organic_unit VARCHAR(30) NOT NULL,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NULL,
    type VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    parent_organic_unit_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id_organic_unit),
    UNIQUE KEY uq_organic_unit_org_code (id_organization, cod_organic_unit),
    KEY idx_organic_unit_org (id_organization),
    KEY idx_organic_unit_parent (parent_organic_unit_id),
    CONSTRAINT fk_organic_unit_org
        FOREIGN KEY (id_organization) REFERENCES organization (id_organization)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_organic_unit_parent
        FOREIGN KEY (parent_organic_unit_id) REFERENCES organic_unit (id_organic_unit)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_organic_unit_type
        CHECK (type IN ('school', 'faculty', 'department', 'center', 'office', 'service', 'section', 'direction', 'other')),
    CONSTRAINT ck_organic_unit_state
        CHECK (state IN ('active', 'inactive', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course (
    id_course BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_organization BIGINT UNSIGNED NOT NULL,
    id_organic_unit BIGINT UNSIGNED NULL,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NULL,
    description VARCHAR(500) NULL,
    ects DECIMAL(7,2) NULL,
    duration VARCHAR(40) NULL,
    type VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_course),
    KEY idx_course_org (id_organization),
    KEY idx_course_organic_unit (id_organic_unit),
    KEY idx_course_state (state),
    CONSTRAINT fk_course_org
        FOREIGN KEY (id_organization) REFERENCES organization (id_organization)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_course_organic_unit
        FOREIGN KEY (id_organic_unit) REFERENCES organic_unit (id_organic_unit)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_course_type
        CHECK (type IN ('degree', 'master', 'short_course', 'professional_training', 'other')),
    CONSTRAINT ck_course_state
        CHECK (state IN ('active', 'inactive', 'archived')),
    CONSTRAINT ck_course_ects
        CHECK (ects IS NULL OR ects >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS subject (
    id_subject BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NULL,
    description VARCHAR(500) NULL,
    ects DECIMAL(7,2) NULL,
    workload_hours INT NULL,
    PRIMARY KEY (id_subject),
    UNIQUE KEY uq_subject_name (name),
    CONSTRAINT ck_subject_ects
        CHECK (ects IS NULL OR ects >= 0),
    CONSTRAINT ck_subject_workload_hours
        CHECK (workload_hours IS NULL OR workload_hours >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS class_group (
    id_class_group BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_subject BIGINT UNSIGNED NOT NULL,
    id_course BIGINT UNSIGNED NOT NULL,
    cod_class_group VARCHAR(30) NOT NULL,
    modality VARCHAR(30) NOT NULL,
    state VARCHAR(20) NOT NULL,
    min_students INT NULL,
    max_students INT NULL,
    starts_at DATE NULL,
    ends_at DATE NULL,
    shift VARCHAR(30) NULL,
    PRIMARY KEY (id_class_group),
    UNIQUE KEY uq_class_group_subject_code (id_subject, cod_class_group),
    KEY idx_class_group_course (id_course),
    KEY idx_class_group_state (state),
    CONSTRAINT fk_class_group_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_class_group_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_class_group_modality
        CHECK (modality IN ('onsite', 'online', 'hybrid')),
    CONSTRAINT ck_class_group_state
        CHECK (state IN ('active', 'inactive', 'closed', 'archived')),
    CONSTRAINT ck_class_group_students_range
        CHECK (
            min_students IS NULL
            OR max_students IS NULL
            OR min_students <= max_students
        ),
    CONSTRAINT ck_class_group_students_pair
        CHECK (
            (min_students IS NULL AND max_students IS NULL)
            OR (min_students IS NOT NULL AND max_students IS NOT NULL)
        ),
    CONSTRAINT ck_class_group_dates_pair
        CHECK (
            (starts_at IS NULL AND ends_at IS NULL)
            OR (starts_at IS NOT NULL AND ends_at IS NOT NULL)
        ),
    CONSTRAINT ck_class_group_dates
        CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at >= starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS content_block (
    id_content_block BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_class_group BIGINT UNSIGNED NOT NULL,
    cod_content_block VARCHAR(30) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    order_no INT NOT NULL,
    access_mode VARCHAR(30) NOT NULL,
    state VARCHAR(20) NOT NULL,
    available_from DATETIME NULL,
    available_until DATETIME NULL,
    PRIMARY KEY (id_content_block),
    UNIQUE KEY uq_content_block_group_code (id_class_group, cod_content_block),
    UNIQUE KEY uq_content_block_group_order (id_class_group, order_no),
    KEY idx_content_block_state (state),
    CONSTRAINT fk_content_block_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_content_block_order
        CHECK (order_no > 0),
    CONSTRAINT ck_content_block_access_mode
        CHECK (access_mode IN ('open', 'restricted', 'scheduled')),
    CONSTRAINT ck_content_block_state
        CHECK (state IN ('draft', 'active', 'inactive', 'archived')),
    CONSTRAINT ck_content_block_available_pair
        CHECK (available_until IS NULL OR available_from IS NOT NULL),
    CONSTRAINT ck_content_block_scheduled_access
        CHECK (access_mode <> 'scheduled' OR available_from IS NOT NULL),
    CONSTRAINT ck_content_block_availability
        CHECK (
            available_from IS NULL
            OR available_until IS NULL
            OR available_until >= available_from
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS integrate_subject (
    id_course BIGINT UNSIGNED NOT NULL,
    id_subject BIGINT UNSIGNED NOT NULL,
    curricular_year INT NULL,
    term VARCHAR(20) NULL,
    mandatory BOOLEAN NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_course, id_subject),
    KEY idx_integrate_subject_subject (id_subject),
    CONSTRAINT fk_integrate_subject_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_integrate_subject_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_integrate_subject_year
        CHECK (curricular_year IS NULL OR curricular_year > 0),
    CONSTRAINT ck_integrate_subject_term
        CHECK (term IS NULL OR term IN ('annual', 'semester_1', 'semester_2', 'trimester_1', 'trimester_2', 'trimester_3')),
    CONSTRAINT ck_integrate_subject_curricular_position
        CHECK (
            (curricular_year IS NULL AND term IS NULL)
            OR (curricular_year IS NOT NULL AND term IS NOT NULL)
        ),
    CONSTRAINT ck_integrate_subject_state
        CHECK (state IN ('active', 'inactive', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS manage_organization (
    id_admin_user BIGINT UNSIGNED NOT NULL,
    id_organization BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    PRIMARY KEY (id_admin_user, id_organization),
    KEY idx_manage_organization_org (id_organization),
    CONSTRAINT fk_manage_organization_admin
        FOREIGN KEY (id_admin_user) REFERENCES administrator_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_manage_organization_org
        FOREIGN KEY (id_organization) REFERENCES organization (id_organization)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_manage_organization_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_manage_organization_state
        CHECK (state IN ('active', 'inactive', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS coordinate_subject (
    id_coordinator_user BIGINT UNSIGNED NOT NULL,
    id_subject BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    PRIMARY KEY (id_coordinator_user, id_subject),
    KEY idx_coordinate_subject_subject (id_subject),
    CONSTRAINT fk_coordinate_subject_coordinator
        FOREIGN KEY (id_coordinator_user) REFERENCES coordinator_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_coordinate_subject_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_coordinate_subject_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS teach_class_group (
    id_teacher_user BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    PRIMARY KEY (id_teacher_user, id_class_group),
    KEY idx_teach_class_group_class_group (id_class_group),
    CONSTRAINT fk_teach_class_group_teacher
        FOREIGN KEY (id_teacher_user) REFERENCES teacher_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_teach_class_group_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_teach_class_group_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enroll_course (
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_course BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    PRIMARY KEY (id_student_user, id_course),
    KEY idx_enroll_course_course (id_course),
    CONSTRAINT fk_enroll_course_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enroll_course_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_enroll_course_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enroll_subject (
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_subject BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    PRIMARY KEY (id_student_user, id_subject),
    KEY idx_enroll_subject_subject (id_subject),
    CONSTRAINT fk_enroll_subject_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enroll_subject_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_enroll_subject_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enroll_class_group (
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    PRIMARY KEY (id_student_user, id_class_group),
    KEY idx_enroll_class_group_class_group (id_class_group),
    CONSTRAINT fk_enroll_class_group_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enroll_class_group_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_enroll_class_group_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 3) CONTEUDOS, AULAS E AVALIACAO
-- =========================================================

CREATE TABLE IF NOT EXISTS content_item (
    id_content_item BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    format VARCHAR(40) NOT NULL,
    source VARCHAR(1000) NULL,
    state VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id_content_item),
    KEY idx_content_item_author (author_user_id),
    KEY idx_content_item_format (format),
    KEY idx_content_item_state (state),
    CONSTRAINT fk_content_item_author
        FOREIGN KEY (author_user_id) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_content_item_format
        CHECK (format IN ('text', 'image', 'video', 'audio', 'pdf', 'url', 'scorm', 'xapi', 'presentation', 'embed', 'other')),
    CONSTRAINT ck_content_item_state
        CHECK (state IN ('draft', 'active', 'inactive', 'archived')),
    CONSTRAINT ck_content_item_updated
        CHECK (updated_at IS NULL OR updated_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS physical_room (
    cod_physical_room VARCHAR(40) NOT NULL,
    id_organization BIGINT UNSIGNED NOT NULL,
    id_organic_unit BIGINT UNSIGNED NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    capacity INT NULL,
    location VARCHAR(255) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (cod_physical_room),
    KEY idx_physical_room_org (id_organization),
    KEY idx_physical_room_organic_unit (id_organic_unit),
    KEY idx_physical_room_state (state),
    CONSTRAINT fk_physical_room_org
        FOREIGN KEY (id_organization) REFERENCES organization (id_organization)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_physical_room_organic_unit
        FOREIGN KEY (id_organic_unit) REFERENCES organic_unit (id_organic_unit)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_physical_room_capacity
        CHECK (capacity IS NULL OR capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS lesson (
    id_lesson BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_class_group BIGINT UNSIGNED NOT NULL,
    id_content_block BIGINT UNSIGNED NOT NULL,
    cod_physical_room VARCHAR(40) NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(30) NOT NULL,
    provider VARCHAR(40) NULL,
    access_url VARCHAR(255) NULL,
    attendance_required BOOLEAN NOT NULL,
    state VARCHAR(20) NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    PRIMARY KEY (id_lesson),
    KEY idx_lesson_class_group (id_class_group),
    KEY idx_lesson_content_block (id_content_block),
    KEY idx_lesson_room (cod_physical_room),
    KEY idx_lesson_state (state),
    CONSTRAINT fk_lesson_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_content_block
        FOREIGN KEY (id_content_block) REFERENCES content_block (id_content_block)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_lesson_room
        FOREIGN KEY (cod_physical_room) REFERENCES physical_room (cod_physical_room)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_lesson_type
        CHECK (type IN ('online', 'onsite', 'hybrid')),
    CONSTRAINT ck_lesson_state
        CHECK (state IN ('scheduled', 'active', 'completed', 'cancelled')),
    CONSTRAINT ck_lesson_dates
        CHECK (ends_at >= starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assessment (
    id_assessment BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_subject BIGINT UNSIGNED NULL,
    id_content_block BIGINT UNSIGNED NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(30) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    correction_mode VARCHAR(30) NOT NULL,
    max_grade DECIMAL(5,2) NOT NULL,
    passing_grade DECIMAL(5,2) NOT NULL,
    attempts_limit INT NULL,
    state VARCHAR(20) NOT NULL,
    available_from DATETIME NULL,
    available_until DATETIME NULL,
    PRIMARY KEY (id_assessment),
    KEY idx_assessment_subject (id_subject),
    KEY idx_assessment_content_block (id_content_block),
    KEY idx_assessment_state (state),
    CONSTRAINT fk_assessment_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_assessment_content_block
        FOREIGN KEY (id_content_block) REFERENCES content_block (id_content_block)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_assessment_type
        CHECK (type IN ('questionnaire', 'exam')),
    CONSTRAINT ck_assessment_grades
        CHECK (max_grade >= 0 AND passing_grade >= 0 AND passing_grade <= max_grade),
    CONSTRAINT ck_assessment_attempts_limit
        CHECK (attempts_limit IS NULL OR attempts_limit > 0),
    CONSTRAINT ck_assessment_availability
        CHECK (
            available_from IS NULL
            OR available_until IS NULL
            OR available_until >= available_from
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question (
    id_question BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_assessment BIGINT UNSIGNED NOT NULL,
    cod_question VARCHAR(30) NOT NULL,
    statement VARCHAR(2000) NOT NULL,
    type VARCHAR(40) NOT NULL,
    order_no INT NOT NULL,
    required_flag BOOLEAN NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    expected_answer VARCHAR(2000) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_question),
    UNIQUE KEY uq_question_assessment_code (id_assessment, cod_question),
    UNIQUE KEY uq_question_assessment_order (id_assessment, order_no),
    KEY idx_question_type (type),
    CONSTRAINT fk_question_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_question_type
        CHECK (type IN ('single_choice', 'multiple_choice', 'dropdown', 'short_text', 'paragraph', 'file_upload', 'date_time', 'rating', 'other')),
    CONSTRAINT ck_question_order
        CHECK (order_no > 0),
    CONSTRAINT ck_question_score
        CHECK (score >= 0),
    CONSTRAINT ck_question_state
        CHECK (state IN ('active', 'inactive', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_option (
    id_option BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_question BIGINT UNSIGNED NOT NULL,
    order_no INT NOT NULL,
    text VARCHAR(300) NOT NULL,
    correct_flag BOOLEAN NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_option),
    UNIQUE KEY uq_question_option_order (id_question, order_no),
    CONSTRAINT fk_question_option_question
        FOREIGN KEY (id_question) REFERENCES question (id_question)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_question_option_order
        CHECK (order_no > 0),
    CONSTRAINT ck_question_option_state
        CHECK (state IN ('active', 'inactive', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attempt (
    id_attempt BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_assessment BIGINT UNSIGNED NOT NULL,
    attempt_number INT NOT NULL,
    score DECIMAL(5,2) NULL,
    state VARCHAR(20) NOT NULL,
    started_at DATETIME NOT NULL,
    submitted_at DATETIME NULL,
    PRIMARY KEY (id_attempt),
    UNIQUE KEY uq_attempt_student_assessment_number (id_student_user, id_assessment, attempt_number),
    KEY idx_attempt_assessment (id_assessment),
    KEY idx_attempt_state (state),
    CONSTRAINT fk_attempt_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_attempt_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_attempt_number
        CHECK (attempt_number > 0),
    CONSTRAINT ck_attempt_score
        CHECK (score IS NULL OR score >= 0),
    CONSTRAINT ck_attempt_state
        CHECK (state IN ('in_progress', 'submitted', 'corrected', 'expired', 'cancelled')),
    CONSTRAINT ck_attempt_submitted
        CHECK (submitted_at IS NULL OR submitted_at >= started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS response (
    id_response BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_attempt BIGINT UNSIGNED NOT NULL,
    id_question BIGINT UNSIGNED NOT NULL,
    cod_response VARCHAR(30) NOT NULL,
    answer VARCHAR(2000) NULL,
    attachment VARCHAR(255) NULL,
    score DECIMAL(5,2) NULL,
    answered_at DATETIME NULL,
    PRIMARY KEY (id_response),
    UNIQUE KEY uq_response_attempt_code (id_attempt, cod_response),
    UNIQUE KEY uq_response_attempt_question (id_attempt, id_question),
    KEY idx_response_question (id_question),
    CONSTRAINT fk_response_attempt
        FOREIGN KEY (id_attempt) REFERENCES attempt (id_attempt)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_response_question
        FOREIGN KEY (id_question) REFERENCES question (id_question)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_response_score
        CHECK (score IS NULL OR score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS response_option (
    id_response BIGINT UNSIGNED NOT NULL,
    id_option BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_response, id_option),
    KEY idx_response_option_option (id_option),
    CONSTRAINT fk_response_option_response
        FOREIGN KEY (id_response) REFERENCES response (id_response)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_response_option_option
        FOREIGN KEY (id_option) REFERENCES question_option (id_option)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_organization_content (
    id_organization BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_organization, id_content_item),
    KEY idx_aoc_content_item (id_content_item),
    CONSTRAINT fk_aoc_organization
        FOREIGN KEY (id_organization) REFERENCES organization (id_organization)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_aoc_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_organic_unit_content (
    id_organic_unit BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_organic_unit, id_content_item),
    KEY idx_aouc_content_item (id_content_item),
    CONSTRAINT fk_aouc_organic_unit
        FOREIGN KEY (id_organic_unit) REFERENCES organic_unit (id_organic_unit)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_aouc_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_course_content (
    id_course BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_course, id_content_item),
    KEY idx_acc_content_item (id_content_item),
    CONSTRAINT fk_acc_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_acc_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_subject_content (
    id_subject BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_subject, id_content_item),
    KEY idx_asc_content_item (id_content_item),
    CONSTRAINT fk_asc_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asc_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_class_group_content (
    id_class_group BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_class_group, id_content_item),
    KEY idx_acgc_content_item (id_content_item),
    CONSTRAINT fk_acgc_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_acgc_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_block_content (
    id_content_block BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    order_no INT NULL,
    role VARCHAR(40) NOT NULL,
    mandatory BOOLEAN NOT NULL,
    PRIMARY KEY (id_content_block, id_content_item),
    KEY idx_abc_content_item (id_content_item),
    CONSTRAINT fk_abc_content_block
        FOREIGN KEY (id_content_block) REFERENCES content_block (id_content_block)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_abc_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_abc_order
        CHECK (order_no IS NULL OR order_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_assessment_content (
    id_assessment BIGINT UNSIGNED NOT NULL,
    id_content_item BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_assessment, id_content_item),
    KEY idx_aac_content_item (id_content_item),
    CONSTRAINT fk_aac_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_aac_content_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =========================================================
-- 4) HORARIOS, ASSIDUIDADE, RESULTADOS, CERTIFICACAO E
--    SERVICOS TRANSVERSAIS
-- =========================================================

CREATE TABLE IF NOT EXISTS schedule_event (
    id_schedule_event BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_lesson BIGINT UNSIGNED NULL,
    id_assessment BIGINT UNSIGNED NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(40) NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    all_day BOOLEAN NOT NULL,
    reminder_enabled BOOLEAN NULL,
    reminder_minutes_before INT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_schedule_event),
    UNIQUE KEY uq_schedule_event_lesson (id_lesson),
    UNIQUE KEY uq_schedule_event_assessment (id_assessment),
    KEY idx_schedule_event_type (type),
    KEY idx_schedule_event_state (state),
    CONSTRAINT fk_schedule_event_lesson
        FOREIGN KEY (id_lesson) REFERENCES lesson (id_lesson)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_schedule_event_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_schedule_event_type
        CHECK (type IN ('lesson', 'assessment', 'reminder', 'meeting', 'other')),
    CONSTRAINT ck_schedule_event_dates
        CHECK (ends_at >= starts_at),
    CONSTRAINT ck_schedule_event_reminder
        CHECK (COALESCE(reminder_enabled, 0) = 0 OR reminder_minutes_before IS NOT NULL),
    CONSTRAINT ck_schedule_event_reminder_minutes
        CHECK (reminder_minutes_before IS NULL OR reminder_minutes_before >= 0),
    CONSTRAINT ck_schedule_event_state
        CHECK (state IN ('draft', 'active', 'inactive', 'cancelled', 'archived', 'completed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS receive_schedule_event (
    id_user BIGINT UNSIGNED NOT NULL,
    id_schedule_event BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_user, id_schedule_event),
    KEY idx_receive_schedule_event_event (id_schedule_event),
    CONSTRAINT fk_receive_schedule_event_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_receive_schedule_event_event
        FOREIGN KEY (id_schedule_event) REFERENCES schedule_event (id_schedule_event)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_schedule_event_class_group (
    id_schedule_event BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_schedule_event, id_class_group),
    KEY idx_asecg_class_group (id_class_group),
    CONSTRAINT fk_asecg_event
        FOREIGN KEY (id_schedule_event) REFERENCES schedule_event (id_schedule_event)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_asecg_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attendance_record (
    id_attendance_record BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_lesson BIGINT UNSIGNED NOT NULL,
    id_user_student BIGINT UNSIGNED NOT NULL,
    status VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL,
    check_in DATETIME NULL,
    check_out DATETIME NULL,
    notes VARCHAR(500) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_attendance_record),
    UNIQUE KEY uq_attendance_lesson_student (id_lesson, id_user_student),
    KEY idx_attendance_state (state),
    CONSTRAINT fk_attendance_lesson
        FOREIGN KEY (id_lesson) REFERENCES lesson (id_lesson)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_student
        FOREIGN KEY (id_user_student) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_attendance_status
        CHECK (status IN ('present', 'absent', 'justified', 'late', 'partial')),
    CONSTRAINT ck_attendance_source
        CHECK (source IN ('manual', 'automatic', 'other')),
    CONSTRAINT ck_attendance_state
        CHECK (state IN ('active', 'corrected', 'cancelled', 'archived')),
    CONSTRAINT ck_attendance_checkout
        CHECK (check_out IS NULL OR check_in IS NULL OR check_out >= check_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS absence_justification (
    id_absence_justification BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_attendance_record BIGINT UNSIGNED NOT NULL,
    id_user_student_submitter BIGINT UNSIGNED NOT NULL,
    id_user_processor BIGINT UNSIGNED NULL,
    submitted_at DATETIME NOT NULL,
    reason VARCHAR(300) NOT NULL,
    attachment VARCHAR(255) NULL,
    processed_at DATETIME NULL,
    decision_notes VARCHAR(500) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_absence_justification),
    UNIQUE KEY uq_absence_justification_attendance (id_attendance_record),
    KEY idx_absence_justification_submitter (id_user_student_submitter),
    KEY idx_absence_justification_processor (id_user_processor),
    CONSTRAINT fk_absence_justification_attendance
        FOREIGN KEY (id_attendance_record) REFERENCES attendance_record (id_attendance_record)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_absence_justification_submitter
        FOREIGN KEY (id_user_student_submitter) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_absence_justification_processor
        FOREIGN KEY (id_user_processor) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_absence_justification_state
        CHECK (state IN ('submitted', 'under_review', 'approved', 'rejected', 'cancelled')),
    CONSTRAINT ck_absence_justification_final_state
        CHECK (state NOT IN ('approved', 'rejected') OR processed_at IS NOT NULL),
    CONSTRAINT ck_absence_justification_processed
        CHECK (processed_at IS NULL OR processed_at >= submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grade_sheet (
    id_grade_sheet BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_subject BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    type VARCHAR(40) NOT NULL,
    released_at DATETIME NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_grade_sheet),
    KEY idx_grade_sheet_subject (id_subject),
    KEY idx_grade_sheet_state (state),
    CONSTRAINT fk_grade_sheet_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_grade_sheet_type
        CHECK (type IN ('final', 'continuous_assessment', 'exam', 'partial', 'other')),
    CONSTRAINT ck_grade_sheet_state
        CHECK (state IN ('draft', 'published', 'closed', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_grade_sheet_class_group (
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_grade_sheet, id_class_group),
    KEY idx_agscg_class_group (id_class_group),
    CONSTRAINT fk_agscg_grade_sheet
        FOREIGN KEY (id_grade_sheet) REFERENCES grade_sheet (id_grade_sheet)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_agscg_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS based_on_assessment (
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    id_assessment BIGINT UNSIGNED NOT NULL,
    weight DECIMAL(5,2) NULL,
    PRIMARY KEY (id_grade_sheet, id_assessment),
    KEY idx_based_on_assessment_assessment (id_assessment),
    CONSTRAINT fk_based_on_assessment_grade_sheet
        FOREIGN KEY (id_grade_sheet) REFERENCES grade_sheet (id_grade_sheet)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_based_on_assessment_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_based_on_assessment_weight
        CHECK (weight IS NULL OR (weight > 0 AND weight <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grade_record (
    id_grade_record BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    id_user_student BIGINT UNSIGNED NOT NULL,
    id_attempt BIGINT UNSIGNED NULL,
    cod_grade_record VARCHAR(30) NOT NULL,
    value DECIMAL(5,2) NOT NULL,
    result VARCHAR(20) NOT NULL,
    recorded_at DATETIME NOT NULL,
    notes VARCHAR(500) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_grade_record),
    UNIQUE KEY uq_grade_record_sheet_code (id_grade_sheet, cod_grade_record),
    UNIQUE KEY uq_grade_record_attempt (id_attempt),
    KEY idx_grade_record_student (id_user_student),
    KEY idx_grade_record_state (state),
    CONSTRAINT fk_grade_record_grade_sheet
        FOREIGN KEY (id_grade_sheet) REFERENCES grade_sheet (id_grade_sheet)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_grade_record_student
        FOREIGN KEY (id_user_student) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_grade_record_attempt
        FOREIGN KEY (id_attempt) REFERENCES attempt (id_attempt)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_grade_record_result
        CHECK (result IN ('approved', 'failed', 'pending', 'absent', 'reproved')),
    CONSTRAINT ck_grade_record_state
        CHECK (state IN ('draft', 'published', 'corrected', 'archived', 'active')),
    CONSTRAINT ck_grade_record_value
        CHECK (value >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS certificate (
    id_certificate BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_course BIGINT UNSIGNED NOT NULL,
    id_user_student BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    notes VARCHAR(500) NULL,
    type VARCHAR(40) NOT NULL,
    template VARCHAR(255) NULL,
    validation_code VARCHAR(80) NULL,
    issued_at DATETIME NULL,
    final_grade DECIMAL(5,2) NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_certificate),
    UNIQUE KEY uq_certificate_validation_code (validation_code),
    KEY idx_certificate_course (id_course),
    KEY idx_certificate_student (id_user_student),
    KEY idx_certificate_state (state),
    CONSTRAINT fk_certificate_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_certificate_student
        FOREIGN KEY (id_user_student) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_certificate_type
        CHECK (type IN ('completion', 'attendance', 'qualification', 'other')),
    CONSTRAINT ck_certificate_state
        CHECK (state IN ('draft', 'active', 'issued', 'revoked', 'archived')),
    CONSTRAINT ck_certificate_issued_context
        CHECK (state <> 'issued' OR (validation_code IS NOT NULL AND issued_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS based_on_grade_sheet_certificate (
    id_certificate BIGINT UNSIGNED NOT NULL,
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_certificate, id_grade_sheet),
    KEY idx_bgsc_grade_sheet (id_grade_sheet),
    CONSTRAINT fk_bgsc_certificate
        FOREIGN KEY (id_certificate) REFERENCES certificate (id_certificate)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_bgsc_grade_sheet
        FOREIGN KEY (id_grade_sheet) REFERENCES grade_sheet (id_grade_sheet)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS management_view (
    id_management_view BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    type VARCHAR(40) NOT NULL,
    description VARCHAR(500) NULL,
    visibility_scope VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_management_view),
    KEY idx_management_view_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS access_management_view (
    id_user BIGINT UNSIGNED NOT NULL,
    id_management_view BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_user, id_management_view),
    KEY idx_access_management_view_view (id_management_view),
    CONSTRAINT fk_access_management_view_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_access_management_view_view
        FOREIGN KEY (id_management_view) REFERENCES management_view (id_management_view)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS channel (
    id_channel BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(160) NOT NULL,
    type VARCHAR(40) NOT NULL,
    visibility VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_channel),
    KEY idx_channel_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS participate_channel (
    id_user BIGINT UNSIGNED NOT NULL,
    id_channel BIGINT UNSIGNED NOT NULL,
    role VARCHAR(40) NOT NULL,
    joined_at DATETIME NOT NULL,
    muted BOOLEAN NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_user, id_channel),
    KEY idx_participate_channel_channel (id_channel),
    CONSTRAINT fk_participate_channel_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_participate_channel_channel
        FOREIGN KEY (id_channel) REFERENCES channel (id_channel)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS message (
    id_message BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_channel BIGINT UNSIGNED NOT NULL,
    id_user_sender BIGINT UNSIGNED NULL,
    id_parent_message BIGINT UNSIGNED NULL,
    id_schedule_event_origin BIGINT UNSIGNED NULL,
    title VARCHAR(160) NULL,
    body VARCHAR(3000) NULL,
    type VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NULL,
    attachment VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    scheduled_at DATETIME NULL,
    sent_at DATETIME NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_message),
    KEY idx_message_channel (id_channel),
    KEY idx_message_sender (id_user_sender),
    KEY idx_message_parent (id_parent_message),
    KEY idx_message_schedule_event_origin (id_schedule_event_origin),
    KEY idx_message_state (state),
    CONSTRAINT fk_message_channel
        FOREIGN KEY (id_channel) REFERENCES channel (id_channel)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_message_sender
        FOREIGN KEY (id_user_sender) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_message_parent
        FOREIGN KEY (id_parent_message) REFERENCES message (id_message)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_message_schedule_event_origin
        FOREIGN KEY (id_schedule_event_origin) REFERENCES schedule_event (id_schedule_event)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_message_type
        CHECK (type IN ('text', 'comment', 'announcement', 'warning', 'alert', 'reminder', 'notification', 'system', 'attachment', 'other')),
    CONSTRAINT ck_message_priority
        CHECK (priority IS NULL OR priority IN ('low', 'normal', 'high', 'urgent')),
    CONSTRAINT ck_message_state
        CHECK (state IN ('draft', 'scheduled', 'sent', 'active', 'edited', 'deleted', 'cancelled', 'archived')),
    CONSTRAINT ck_message_updated
        CHECK (updated_at IS NULL OR updated_at >= created_at),
    CONSTRAINT ck_message_scheduled
        CHECK (scheduled_at IS NULL OR scheduled_at >= created_at),
    CONSTRAINT ck_message_scheduled_state
        CHECK (state <> 'scheduled' OR scheduled_at IS NOT NULL),
    CONSTRAINT ck_message_sent
        CHECK (sent_at IS NULL OR sent_at >= created_at),
    CONSTRAINT ck_message_sent_state
        CHECK (state <> 'sent' OR sent_at IS NOT NULL),
    CONSTRAINT ck_message_attachment_type
        CHECK (type <> 'attachment' OR attachment IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS receive_message (
    id_user BIGINT UNSIGNED NOT NULL,
    id_message BIGINT UNSIGNED NOT NULL,
    delivered_at DATETIME NULL,
    read_at DATETIME NULL,
    delivery_mode VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_user, id_message),
    KEY idx_receive_message_message (id_message),
    CONSTRAINT fk_receive_message_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_receive_message_message
        FOREIGN KEY (id_message) REFERENCES message (id_message)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_receive_message_delivery_mode
        CHECK (delivery_mode IN ('internal', 'email', 'both')),
    CONSTRAINT ck_receive_message_state
        CHECK (state IN ('pending', 'delivered', 'read', 'failed', 'archived')),
    CONSTRAINT ck_receive_message_read_after_delivered
        CHECK (read_at IS NULL OR delivered_at IS NULL OR read_at >= delivered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_channel_class_group (
    id_channel BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_channel, id_class_group),
    KEY idx_accg_class_group (id_class_group),
    CONSTRAINT fk_accg_channel
        FOREIGN KEY (id_channel) REFERENCES channel (id_channel)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_accg_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_channel_content_block (
    id_channel BIGINT UNSIGNED NOT NULL,
    id_content_block BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_channel, id_content_block),
    KEY idx_accb_content_block (id_content_block),
    CONSTRAINT fk_accb_channel
        FOREIGN KEY (id_channel) REFERENCES channel (id_channel)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_accb_content_block
        FOREIGN KEY (id_content_block) REFERENCES content_block (id_content_block)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS associate_channel_assessment (
    id_channel BIGINT UNSIGNED NOT NULL,
    id_assessment BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_channel, id_assessment),
    KEY idx_aca_assessment (id_assessment),
    CONSTRAINT fk_aca_channel
        FOREIGN KEY (id_channel) REFERENCES channel (id_channel)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_aca_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS activity_log (
    id_activity_log BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_user BIGINT UNSIGNED NULL,
    id_session BIGINT UNSIGNED NULL,
    operation_type VARCHAR(40) NOT NULL,
    affected_entity_type VARCHAR(80) NOT NULL,
    affected_entity_identifier VARCHAR(120) NOT NULL,
    occurred_at DATETIME NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    source_ip VARCHAR(45) NULL,
    PRIMARY KEY (id_activity_log),
    KEY idx_activity_log_user (id_user),
    KEY idx_activity_log_session (id_session),
    KEY idx_activity_log_occurred_at (occurred_at),
    CONSTRAINT fk_activity_log_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_activity_log_session
        FOREIGN KEY (id_session) REFERENCES user_session (id_session)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DELIMITER $$

DROP TRIGGER IF EXISTS bi_organization_active_admin$$
CREATE TRIGGER bi_organization_active_admin
BEFORE INSERT ON organization
FOR EACH ROW
BEGIN
    IF NEW.state = 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Organization must be activated after administrator assignment';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_organization_active_admin$$
CREATE TRIGGER bu_organization_active_admin
BEFORE UPDATE ON organization
FOR EACH ROW
BEGIN
    DECLARE v_active_admins INT DEFAULT 0;

    IF NEW.state = 'active' THEN
        SELECT COUNT(*)
        INTO v_active_admins
        FROM manage_organization mo
        JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
        JOIN user_account u ON u.id_user = ap.id_user
        WHERE mo.id_organization = NEW.id_organization
          AND mo.state = 'active'
          AND u.state = 'active'
          AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
          AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE);

        IF v_active_admins = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Organization requires at least one active Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_user_account_organization_admin_guard$$
CREATE TRIGGER bu_user_account_organization_admin_guard
BEFORE UPDATE ON user_account
FOR EACH ROW
BEGIN
    DECLARE v_invalid_organizations INT DEFAULT 0;

    IF OLD.state = 'active' AND NEW.state <> 'active' THEN
        SELECT COUNT(*)
        INTO v_invalid_organizations
        FROM organization o
        JOIN manage_organization mo_old ON mo_old.id_organization = o.id_organization
        WHERE o.state = 'active'
          AND mo_old.id_admin_user = OLD.id_user
          AND mo_old.state = 'active'
          AND (mo_old.start_date IS NULL OR mo_old.start_date <= CURRENT_DATE)
          AND (mo_old.end_date IS NULL OR mo_old.end_date >= CURRENT_DATE)
          AND NOT EXISTS (
              SELECT 1
              FROM manage_organization mo
              JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
              JOIN user_account u ON u.id_user = ap.id_user
              WHERE mo.id_organization = o.id_organization
                AND mo.id_admin_user <> OLD.id_user
                AND mo.state = 'active'
                AND u.state = 'active'
                AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
          );

        IF v_invalid_organizations > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User state change would leave active Organization without active Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_user_account_organization_admin_guard$$
CREATE TRIGGER bd_user_account_organization_admin_guard
BEFORE DELETE ON user_account
FOR EACH ROW
BEGIN
    DECLARE v_invalid_organizations INT DEFAULT 0;

    IF OLD.state = 'active' THEN
        SELECT COUNT(*)
        INTO v_invalid_organizations
        FROM organization o
        JOIN manage_organization mo_old ON mo_old.id_organization = o.id_organization
        WHERE o.state = 'active'
          AND mo_old.id_admin_user = OLD.id_user
          AND mo_old.state = 'active'
          AND (mo_old.start_date IS NULL OR mo_old.start_date <= CURRENT_DATE)
          AND (mo_old.end_date IS NULL OR mo_old.end_date >= CURRENT_DATE)
          AND NOT EXISTS (
              SELECT 1
              FROM manage_organization mo
              JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
              JOIN user_account u ON u.id_user = ap.id_user
              WHERE mo.id_organization = o.id_organization
                AND mo.id_admin_user <> OLD.id_user
                AND mo.state = 'active'
                AND u.state = 'active'
                AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
          );

        IF v_invalid_organizations > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User deletion would leave active Organization without active Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_administrator_profile_organization_admin_guard$$
CREATE TRIGGER bd_administrator_profile_organization_admin_guard
BEFORE DELETE ON administrator_profile
FOR EACH ROW
BEGIN
    DECLARE v_user_state VARCHAR(20);
    DECLARE v_invalid_organizations INT DEFAULT 0;

    SELECT state
    INTO v_user_state
    FROM user_account
    WHERE id_user = OLD.id_user;

    IF v_user_state = 'active' THEN
        SELECT COUNT(*)
        INTO v_invalid_organizations
        FROM organization o
        JOIN manage_organization mo_old ON mo_old.id_organization = o.id_organization
        WHERE o.state = 'active'
          AND mo_old.id_admin_user = OLD.id_user
          AND mo_old.state = 'active'
          AND (mo_old.start_date IS NULL OR mo_old.start_date <= CURRENT_DATE)
          AND (mo_old.end_date IS NULL OR mo_old.end_date >= CURRENT_DATE)
          AND NOT EXISTS (
              SELECT 1
              FROM manage_organization mo
              JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
              JOIN user_account u ON u.id_user = ap.id_user
              WHERE mo.id_organization = o.id_organization
                AND mo.id_admin_user <> OLD.id_user
                AND mo.state = 'active'
                AND u.state = 'active'
                AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
                AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE)
          );

        IF v_invalid_organizations > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Administrator profile deletion would leave active Organization without active Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_manage_organization_active_admin$$
CREATE TRIGGER bu_manage_organization_active_admin
BEFORE UPDATE ON manage_organization
FOR EACH ROW
BEGIN
    DECLARE v_org_state VARCHAR(20);
    DECLARE v_active_admins INT DEFAULT 0;

    SELECT state
    INTO v_org_state
    FROM organization
    WHERE id_organization = OLD.id_organization;

    IF v_org_state = 'active' THEN
        SELECT COUNT(*)
        INTO v_active_admins
        FROM manage_organization mo
        JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
        JOIN user_account u ON u.id_user = ap.id_user
        WHERE mo.id_organization = OLD.id_organization
          AND NOT (mo.id_admin_user = OLD.id_admin_user AND mo.id_organization = OLD.id_organization)
          AND mo.state = 'active'
          AND u.state = 'active'
          AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
          AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE);

        IF NEW.id_organization = OLD.id_organization
           AND NEW.state = 'active'
           AND (NEW.start_date IS NULL OR NEW.start_date <= CURRENT_DATE)
           AND (NEW.end_date IS NULL OR NEW.end_date >= CURRENT_DATE)
           AND EXISTS (
               SELECT 1
               FROM administrator_profile ap
               JOIN user_account u ON u.id_user = ap.id_user
               WHERE ap.id_user = NEW.id_admin_user
                 AND u.state = 'active'
           ) THEN
            SET v_active_admins = v_active_admins + 1;
        END IF;

        IF v_active_admins = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assignment update would leave active Organization without active Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_manage_organization_active_admin$$
CREATE TRIGGER bd_manage_organization_active_admin
BEFORE DELETE ON manage_organization
FOR EACH ROW
BEGIN
    DECLARE v_org_state VARCHAR(20);
    DECLARE v_active_admins INT DEFAULT 0;

    SELECT state
    INTO v_org_state
    FROM organization
    WHERE id_organization = OLD.id_organization;

    IF v_org_state = 'active' THEN
        SELECT COUNT(*)
        INTO v_active_admins
        FROM manage_organization mo
        JOIN administrator_profile ap ON ap.id_user = mo.id_admin_user
        JOIN user_account u ON u.id_user = ap.id_user
        WHERE mo.id_organization = OLD.id_organization
          AND NOT (mo.id_admin_user = OLD.id_admin_user AND mo.id_organization = OLD.id_organization)
          AND mo.state = 'active'
          AND u.state = 'active'
          AND (mo.start_date IS NULL OR mo.start_date <= CURRENT_DATE)
          AND (mo.end_date IS NULL OR mo.end_date >= CURRENT_DATE);

        IF v_active_admins = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assignment deletion would leave active Organization without active Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_organic_unit_validate$$
CREATE TRIGGER bi_organic_unit_validate
BEFORE INSERT ON organic_unit
FOR EACH ROW
BEGIN
    DECLARE v_parent_org BIGINT UNSIGNED;

    IF NEW.parent_organic_unit_id IS NOT NULL THEN
        IF NEW.id_organic_unit IS NOT NULL AND NEW.parent_organic_unit_id = NEW.id_organic_unit THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Organic_Unit cannot be subordinated to itself';
        END IF;

        SELECT id_organization
        INTO v_parent_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.parent_organic_unit_id;

        IF v_parent_org IS NULL OR v_parent_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Organic_Unit parent must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_organic_unit_validate$$
CREATE TRIGGER bu_organic_unit_validate
BEFORE UPDATE ON organic_unit
FOR EACH ROW
BEGIN
    DECLARE v_parent_org BIGINT UNSIGNED;
    DECLARE v_cycle_count INT DEFAULT 0;

    IF NEW.parent_organic_unit_id IS NOT NULL THEN
        IF NEW.parent_organic_unit_id = NEW.id_organic_unit THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Organic_Unit cannot be subordinated to itself';
        END IF;

        SELECT id_organization
        INTO v_parent_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.parent_organic_unit_id;

        IF v_parent_org IS NULL OR v_parent_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Organic_Unit parent must belong to the same Organization';
        END IF;

        WITH RECURSIVE parent_chain (id_organic_unit, parent_organic_unit_id) AS (
            SELECT id_organic_unit, parent_organic_unit_id
            FROM organic_unit
            WHERE id_organic_unit = NEW.parent_organic_unit_id
            UNION ALL
            SELECT ou.id_organic_unit, ou.parent_organic_unit_id
            FROM organic_unit ou
            JOIN parent_chain pc ON ou.id_organic_unit = pc.parent_organic_unit_id
        )
        SELECT COUNT(*)
        INTO v_cycle_count
        FROM parent_chain
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_cycle_count > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Organic_Unit hierarchy cannot contain cycles';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_course_validate$$
CREATE TRIGGER bi_course_validate
BEFORE INSERT ON course
FOR EACH ROW
BEGIN
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_course_validate$$
CREATE TRIGGER bu_course_validate
BEFORE UPDATE ON course
FOR EACH ROW
BEGIN
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_class_group_validate$$
CREATE TRIGGER bi_class_group_validate
BEFORE INSERT ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_exists INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = NEW.id_course
      AND id_subject = NEW.id_subject
      AND state <> 'archived';

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course must integrate the selected Subject';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_class_group_validate$$
CREATE TRIGGER bu_class_group_validate
BEFORE UPDATE ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_exists INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = NEW.id_course
      AND id_subject = NEW.id_subject
      AND state <> 'archived';

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course must integrate the selected Subject';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_physical_room_validate$$
CREATE TRIGGER bi_physical_room_validate
BEFORE INSERT ON physical_room
FOR EACH ROW
BEGIN
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_physical_room_validate$$
CREATE TRIGGER bu_physical_room_validate
BEFORE UPDATE ON physical_room
FOR EACH ROW
BEGIN
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_content_item_validate$$
CREATE TRIGGER bi_content_item_validate
BEFORE INSERT ON content_item
FOR EACH ROW
BEGIN
    IF NEW.format IN ('text', 'image', 'video', 'audio', 'pdf', 'url', 'scorm', 'xapi', 'presentation', 'embed')
       AND COALESCE(TRIM(NEW.source), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Content_Item source is required for the selected format';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_content_item_validate$$
CREATE TRIGGER bu_content_item_validate
BEFORE UPDATE ON content_item
FOR EACH ROW
BEGIN
    IF NEW.format IN ('text', 'image', 'video', 'audio', 'pdf', 'url', 'scorm', 'xapi', 'presentation', 'embed')
       AND COALESCE(TRIM(NEW.source), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Content_Item source is required for the selected format';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_lesson_validate$$
CREATE TRIGGER bi_lesson_validate
BEFORE INSERT ON lesson
FOR EACH ROW
BEGIN
    DECLARE v_block_class_group BIGINT UNSIGNED;
    DECLARE v_class_org BIGINT UNSIGNED;
    DECLARE v_room_org BIGINT UNSIGNED;
    DECLARE v_room_state VARCHAR(20);
    DECLARE v_overlap_count INT DEFAULT 0;

    SELECT id_class_group
    INTO v_block_class_group
    FROM content_block
    WHERE id_content_block = NEW.id_content_block;

    IF v_block_class_group <> NEW.id_class_group THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lesson Content_Block must belong to the same Class_Group';
    END IF;

    IF NEW.type = 'online' AND COALESCE(TRIM(NEW.access_url), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Online Lesson requires access_url';
    END IF;

    IF NEW.type = 'onsite' AND NEW.cod_physical_room IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Onsite Lesson requires a Physical_Room';
    END IF;

    IF NEW.type = 'hybrid' AND COALESCE(TRIM(NEW.access_url), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hybrid Lesson requires access_url';
    END IF;

    IF NEW.cod_physical_room IS NOT NULL THEN
        SELECT state, id_organization
        INTO v_room_state, v_room_org
        FROM physical_room
        WHERE cod_physical_room = NEW.cod_physical_room;

        SELECT c.id_organization
        INTO v_class_org
        FROM class_group cg
        JOIN course c ON c.id_course = cg.id_course
        WHERE cg.id_class_group = NEW.id_class_group;

        IF v_room_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lesson Physical_Room must be active';
        END IF;

        IF v_room_org <> v_class_org THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lesson Physical_Room must belong to the same Organization context';
        END IF;

        IF NEW.state = 'active' AND NEW.type IN ('onsite', 'hybrid') THEN
            SELECT COUNT(*)
            INTO v_overlap_count
            FROM lesson l
            WHERE l.cod_physical_room = NEW.cod_physical_room
              AND l.state = 'active'
              AND l.type IN ('onsite', 'hybrid')
              AND NOT (NEW.ends_at <= l.starts_at OR NEW.starts_at >= l.ends_at);

            IF v_overlap_count > 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room already has an overlapping active Lesson';
            END IF;
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_lesson_validate$$
CREATE TRIGGER bu_lesson_validate
BEFORE UPDATE ON lesson
FOR EACH ROW
BEGIN
    DECLARE v_block_class_group BIGINT UNSIGNED;
    DECLARE v_class_org BIGINT UNSIGNED;
    DECLARE v_room_org BIGINT UNSIGNED;
    DECLARE v_room_state VARCHAR(20);
    DECLARE v_overlap_count INT DEFAULT 0;

    SELECT id_class_group
    INTO v_block_class_group
    FROM content_block
    WHERE id_content_block = NEW.id_content_block;

    IF v_block_class_group <> NEW.id_class_group THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lesson Content_Block must belong to the same Class_Group';
    END IF;

    IF NEW.type = 'online' AND COALESCE(TRIM(NEW.access_url), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Online Lesson requires access_url';
    END IF;

    IF NEW.type = 'onsite' AND NEW.cod_physical_room IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Onsite Lesson requires a Physical_Room';
    END IF;

    IF NEW.type = 'hybrid' AND COALESCE(TRIM(NEW.access_url), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hybrid Lesson requires access_url';
    END IF;

    IF NEW.cod_physical_room IS NOT NULL THEN
        SELECT state, id_organization
        INTO v_room_state, v_room_org
        FROM physical_room
        WHERE cod_physical_room = NEW.cod_physical_room;

        SELECT c.id_organization
        INTO v_class_org
        FROM class_group cg
        JOIN course c ON c.id_course = cg.id_course
        WHERE cg.id_class_group = NEW.id_class_group;

        IF v_room_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lesson Physical_Room must be active';
        END IF;

        IF v_room_org <> v_class_org THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Lesson Physical_Room must belong to the same Organization context';
        END IF;

        IF NEW.state = 'active' AND NEW.type IN ('onsite', 'hybrid') THEN
            SELECT COUNT(*)
            INTO v_overlap_count
            FROM lesson l
            WHERE l.cod_physical_room = NEW.cod_physical_room
              AND l.state = 'active'
              AND l.type IN ('onsite', 'hybrid')
              AND l.id_lesson <> NEW.id_lesson
              AND NOT (NEW.ends_at <= l.starts_at OR NEW.starts_at >= l.ends_at);

            IF v_overlap_count > 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room already has an overlapping active Lesson';
            END IF;
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_assessment_validate$$
CREATE TRIGGER bi_assessment_validate
BEFORE INSERT ON assessment
FOR EACH ROW
BEGIN
    DECLARE v_block_subject BIGINT UNSIGNED;

    IF NEW.type = 'questionnaire' AND NEW.id_content_block IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Questionnaire Assessment requires a Content_Block';
    END IF;

    IF NEW.type = 'exam' AND NEW.id_subject IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exam Assessment requires a Subject';
    END IF;

    IF NEW.id_content_block IS NOT NULL AND NEW.id_subject IS NOT NULL THEN
        SELECT cg.id_subject
        INTO v_block_subject
        FROM content_block cb
        JOIN class_group cg ON cg.id_class_group = cb.id_class_group
        WHERE cb.id_content_block = NEW.id_content_block;

        IF v_block_subject <> NEW.id_subject THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment Subject must match the Subject of the referenced Content_Block';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_assessment_validate$$
CREATE TRIGGER bu_assessment_validate
BEFORE UPDATE ON assessment
FOR EACH ROW
BEGIN
    DECLARE v_block_subject BIGINT UNSIGNED;

    IF NEW.type = 'questionnaire' AND NEW.id_content_block IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Questionnaire Assessment requires a Content_Block';
    END IF;

    IF NEW.type = 'exam' AND NEW.id_subject IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exam Assessment requires a Subject';
    END IF;

    IF NEW.id_content_block IS NOT NULL AND NEW.id_subject IS NOT NULL THEN
        SELECT cg.id_subject
        INTO v_block_subject
        FROM content_block cb
        JOIN class_group cg ON cg.id_class_group = cb.id_class_group
        WHERE cb.id_content_block = NEW.id_content_block;

        IF v_block_subject <> NEW.id_subject THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment Subject must match the Subject of the referenced Content_Block';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_attempt_validate$$
CREATE TRIGGER bi_attempt_validate
BEFORE INSERT ON attempt
FOR EACH ROW
BEGIN
    DECLARE v_attempts_limit INT;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_class_group BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT a.attempts_limit, a.id_subject, cb.id_class_group
    INTO v_attempts_limit, v_assessment_subject, v_class_group
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_attempts_limit IS NOT NULL AND NEW.attempt_number > v_attempts_limit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt number exceeds Assessment.attempts_limit';
    END IF;

    IF NEW.state IN ('submitted', 'corrected') AND NEW.submitted_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted or corrected Attempt requires submitted_at';
    END IF;

    IF v_class_group IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM enroll_class_group
        WHERE id_student_user = NEW.id_student_user
          AND id_class_group = v_class_group
          AND state = 'active';
    ELSEIF v_assessment_subject IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM enroll_subject
        WHERE id_student_user = NEW.id_student_user
          AND id_subject = v_assessment_subject
          AND state = 'active';
    END IF;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active enrollment in the Assessment context';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_attempt_validate$$
CREATE TRIGGER bu_attempt_validate
BEFORE UPDATE ON attempt
FOR EACH ROW
BEGIN
    DECLARE v_attempts_limit INT;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_class_group BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT a.attempts_limit, a.id_subject, cb.id_class_group
    INTO v_attempts_limit, v_assessment_subject, v_class_group
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_attempts_limit IS NOT NULL AND NEW.attempt_number > v_attempts_limit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt number exceeds Assessment.attempts_limit';
    END IF;

    IF NEW.state IN ('submitted', 'corrected') AND NEW.submitted_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted or corrected Attempt requires submitted_at';
    END IF;

    IF v_class_group IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM enroll_class_group
        WHERE id_student_user = NEW.id_student_user
          AND id_class_group = v_class_group
          AND state = 'active';
    ELSEIF v_assessment_subject IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM enroll_subject
        WHERE id_student_user = NEW.id_student_user
          AND id_subject = v_assessment_subject
          AND state = 'active';
    END IF;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active enrollment in the Assessment context';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_response_validate$$
CREATE TRIGGER bi_response_validate
BEFORE INSERT ON response
FOR EACH ROW
BEGIN
    DECLARE v_attempt_assessment BIGINT UNSIGNED;
    DECLARE v_question_assessment BIGINT UNSIGNED;

    SELECT id_assessment INTO v_attempt_assessment
    FROM attempt
    WHERE id_attempt = NEW.id_attempt;

    SELECT id_assessment INTO v_question_assessment
    FROM question
    WHERE id_question = NEW.id_question;

    IF v_attempt_assessment <> v_question_assessment THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Response Question must belong to the Assessment of the Attempt';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_response_option_validate$$
CREATE TRIGGER bi_response_option_validate
BEFORE INSERT ON response_option
FOR EACH ROW
BEGIN
    DECLARE v_response_question BIGINT UNSIGNED;
    DECLARE v_option_question BIGINT UNSIGNED;
    DECLARE v_question_type VARCHAR(40);
    DECLARE v_selected_count INT DEFAULT 0;

    SELECT id_question INTO v_response_question
    FROM response
    WHERE id_response = NEW.id_response;

    SELECT id_question INTO v_option_question
    FROM question_option
    WHERE id_option = NEW.id_option;

    IF v_response_question <> v_option_question THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Response_Option must reference an Option of the same Question';
    END IF;

    SELECT type INTO v_question_type
    FROM question
    WHERE id_question = v_response_question;

    IF v_question_type IN ('single_choice', 'dropdown') THEN
        SELECT COUNT(*)
        INTO v_selected_count
        FROM response_option
        WHERE id_response = NEW.id_response;

        IF v_selected_count >= 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Single-choice and dropdown Questions allow at most one selected Option';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_schedule_event_validate$$
CREATE TRIGGER bi_schedule_event_validate
BEFORE INSERT ON schedule_event
FOR EACH ROW
BEGIN
    DECLARE v_lesson_start DATETIME;
    DECLARE v_lesson_end DATETIME;
    DECLARE v_available_from DATETIME;
    DECLARE v_available_until DATETIME;

    IF NEW.id_lesson IS NOT NULL AND NEW.type <> 'lesson' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event linked to Lesson must have type lesson';
    END IF;

    IF NEW.id_assessment IS NOT NULL AND NEW.type <> 'assessment' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event linked to Assessment must have type assessment';
    END IF;

    IF NEW.id_lesson IS NOT NULL THEN
        SELECT starts_at, ends_at
        INTO v_lesson_start, v_lesson_end
        FROM lesson
        WHERE id_lesson = NEW.id_lesson;

        IF NEW.starts_at <> v_lesson_start OR NEW.ends_at <> v_lesson_end THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event period must match the referenced Lesson';
        END IF;
    END IF;

    IF NEW.id_assessment IS NOT NULL THEN
        SELECT available_from, available_until
        INTO v_available_from, v_available_until
        FROM assessment
        WHERE id_assessment = NEW.id_assessment;

        IF v_available_from IS NOT NULL AND NEW.starts_at < v_available_from THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event starts before Assessment availability';
        END IF;

        IF v_available_until IS NOT NULL AND NEW.ends_at > v_available_until THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event ends after Assessment availability';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_schedule_event_validate$$
CREATE TRIGGER bu_schedule_event_validate
BEFORE UPDATE ON schedule_event
FOR EACH ROW
BEGIN
    DECLARE v_lesson_start DATETIME;
    DECLARE v_lesson_end DATETIME;
    DECLARE v_available_from DATETIME;
    DECLARE v_available_until DATETIME;

    IF NEW.id_lesson IS NOT NULL AND NEW.type <> 'lesson' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event linked to Lesson must have type lesson';
    END IF;

    IF NEW.id_assessment IS NOT NULL AND NEW.type <> 'assessment' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event linked to Assessment must have type assessment';
    END IF;

    IF NEW.id_lesson IS NOT NULL THEN
        SELECT starts_at, ends_at
        INTO v_lesson_start, v_lesson_end
        FROM lesson
        WHERE id_lesson = NEW.id_lesson;

        IF NEW.starts_at <> v_lesson_start OR NEW.ends_at <> v_lesson_end THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event period must match the referenced Lesson';
        END IF;
    END IF;

    IF NEW.id_assessment IS NOT NULL THEN
        SELECT available_from, available_until
        INTO v_available_from, v_available_until
        FROM assessment
        WHERE id_assessment = NEW.id_assessment;

        IF v_available_from IS NOT NULL AND NEW.starts_at < v_available_from THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event starts before Assessment availability';
        END IF;

        IF v_available_until IS NOT NULL AND NEW.ends_at > v_available_until THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schedule_Event ends after Assessment availability';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_attendance_record_validate$$
CREATE TRIGGER bi_attendance_record_validate
BEFORE INSERT ON attendance_record
FOR EACH ROW
BEGIN
    DECLARE v_class_group BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT id_class_group
    INTO v_class_group
    FROM lesson
    WHERE id_lesson = NEW.id_lesson;

    SELECT COUNT(*)
    INTO v_exists
    FROM enroll_class_group
    WHERE id_student_user = NEW.id_user_student
      AND id_class_group = v_class_group
      AND state = 'active';

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attendance_Record requires an active enrollment in the Lesson Class_Group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_attendance_record_validate$$
CREATE TRIGGER bu_attendance_record_validate
BEFORE UPDATE ON attendance_record
FOR EACH ROW
BEGIN
    DECLARE v_class_group BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT id_class_group
    INTO v_class_group
    FROM lesson
    WHERE id_lesson = NEW.id_lesson;

    SELECT COUNT(*)
    INTO v_exists
    FROM enroll_class_group
    WHERE id_student_user = NEW.id_user_student
      AND id_class_group = v_class_group
      AND state = 'active';

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attendance_Record requires an active enrollment in the Lesson Class_Group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_absence_justification_validate$$
CREATE TRIGGER bi_absence_justification_validate
BEFORE INSERT ON absence_justification
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(30);
    DECLARE v_student BIGINT UNSIGNED;

    SELECT status, id_user_student
    INTO v_status, v_student
    FROM attendance_record
    WHERE id_attendance_record = NEW.id_attendance_record;

    IF v_status NOT IN ('absent', 'late', 'partial') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Absence_Justification requires an Attendance_Record compatible with absence';
    END IF;

    IF v_student <> NEW.id_user_student_submitter THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Absence_Justification submitter must match the Attendance_Record student';
    END IF;

    IF NEW.state IN ('approved', 'rejected')
       AND (NEW.processed_at IS NULL OR NEW.id_user_processor IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Approved or rejected Absence_Justification requires processor and processed_at';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_absence_justification_validate$$
CREATE TRIGGER bu_absence_justification_validate
BEFORE UPDATE ON absence_justification
FOR EACH ROW
BEGIN
    DECLARE v_status VARCHAR(30);
    DECLARE v_student BIGINT UNSIGNED;

    SELECT status, id_user_student
    INTO v_status, v_student
    FROM attendance_record
    WHERE id_attendance_record = NEW.id_attendance_record;

    IF v_status NOT IN ('absent', 'late', 'partial') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Absence_Justification requires an Attendance_Record compatible with absence';
    END IF;

    IF v_student <> NEW.id_user_student_submitter THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Absence_Justification submitter must match the Attendance_Record student';
    END IF;

    IF NEW.state IN ('approved', 'rejected')
       AND (NEW.processed_at IS NULL OR NEW.id_user_processor IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Approved or rejected Absence_Justification requires processor and processed_at';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_associate_grade_sheet_class_group_validate$$
CREATE TRIGGER bi_associate_grade_sheet_class_group_validate
BEFORE INSERT ON associate_grade_sheet_class_group
FOR EACH ROW
BEGIN
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_class_group_subject BIGINT UNSIGNED;

    SELECT id_subject INTO v_grade_sheet_subject
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    SELECT id_subject INTO v_class_group_subject
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF v_grade_sheet_subject <> v_class_group_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Sheet Class_Groups must belong to the same Subject';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_based_on_assessment_validate$$
CREATE TRIGGER bi_based_on_assessment_validate
BEFORE INSERT ON based_on_assessment
FOR EACH ROW
BEGIN
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_block_subject BIGINT UNSIGNED;
    DECLARE v_total_weight DECIMAL(7,2) DEFAULT 0;

    SELECT id_subject INTO v_grade_sheet_subject
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    SELECT a.id_subject, cg.id_subject
    INTO v_assessment_subject, v_block_subject
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_assessment_subject IS NOT NULL AND v_assessment_subject <> v_grade_sheet_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Based_On_Assessment Assessment Subject must match Grade_Sheet Subject';
    END IF;

    IF v_block_subject IS NOT NULL AND v_block_subject <> v_grade_sheet_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Based_On_Assessment Content_Block Subject must match Grade_Sheet Subject';
    END IF;

    IF NEW.weight IS NOT NULL THEN
        SELECT COALESCE(SUM(weight), 0)
        INTO v_total_weight
        FROM based_on_assessment
        WHERE id_grade_sheet = NEW.id_grade_sheet;

        IF v_total_weight + NEW.weight > 100 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Based_On_Assessment total weight cannot exceed 100';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_based_on_assessment_validate$$
CREATE TRIGGER bu_based_on_assessment_validate
BEFORE UPDATE ON based_on_assessment
FOR EACH ROW
BEGIN
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_block_subject BIGINT UNSIGNED;
    DECLARE v_total_weight DECIMAL(7,2) DEFAULT 0;

    SELECT id_subject INTO v_grade_sheet_subject
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    SELECT a.id_subject, cg.id_subject
    INTO v_assessment_subject, v_block_subject
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_assessment_subject IS NOT NULL AND v_assessment_subject <> v_grade_sheet_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Based_On_Assessment Assessment Subject must match Grade_Sheet Subject';
    END IF;

    IF v_block_subject IS NOT NULL AND v_block_subject <> v_grade_sheet_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Based_On_Assessment Content_Block Subject must match Grade_Sheet Subject';
    END IF;

    IF NEW.weight IS NOT NULL THEN
        SELECT COALESCE(SUM(weight), 0)
        INTO v_total_weight
        FROM based_on_assessment
        WHERE id_grade_sheet = NEW.id_grade_sheet
          AND id_assessment <> OLD.id_assessment;

        IF v_total_weight + NEW.weight > 100 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Based_On_Assessment total weight cannot exceed 100';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_grade_record_validate$$
CREATE TRIGGER bi_grade_record_validate
BEFORE INSERT ON grade_record
FOR EACH ROW
BEGIN
    DECLARE v_attempt_student BIGINT UNSIGNED;
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_block_subject BIGINT UNSIGNED;
    DECLARE v_max_grade DECIMAL(5,2);
    DECLARE v_active_count INT DEFAULT 0;

    IF NEW.id_attempt IS NOT NULL THEN
        SELECT a.id_student_user, gs.id_subject, ass.id_subject, cg.id_subject, ass.max_grade
        INTO v_attempt_student, v_grade_sheet_subject, v_assessment_subject, v_block_subject, v_max_grade
        FROM attempt a
        JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
        JOIN assessment ass ON ass.id_assessment = a.id_assessment
        LEFT JOIN content_block cb ON cb.id_content_block = ass.id_content_block
        LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
        WHERE a.id_attempt = NEW.id_attempt;

        IF v_attempt_student <> NEW.id_user_student THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record Attempt must belong to the same Student';
        END IF;

        IF v_assessment_subject IS NOT NULL AND v_assessment_subject <> v_grade_sheet_subject THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record Attempt Assessment Subject must match Grade_Sheet Subject';
        END IF;

        IF v_block_subject IS NOT NULL AND v_block_subject <> v_grade_sheet_subject THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record Attempt Content_Block Subject must match Grade_Sheet Subject';
        END IF;

        IF v_max_grade IS NOT NULL AND NEW.value > v_max_grade THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record value cannot exceed the Assessment max_grade';
        END IF;
    END IF;

    IF NEW.state = 'active' THEN
        SELECT COUNT(*)
        INTO v_active_count
        FROM grade_record
        WHERE id_grade_sheet = NEW.id_grade_sheet
          AND id_user_student = NEW.id_user_student
          AND state = 'active';

        IF v_active_count > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only one active Grade_Record is allowed per Grade_Sheet and Student';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_grade_record_validate$$
CREATE TRIGGER bu_grade_record_validate
BEFORE UPDATE ON grade_record
FOR EACH ROW
BEGIN
    DECLARE v_attempt_student BIGINT UNSIGNED;
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_block_subject BIGINT UNSIGNED;
    DECLARE v_max_grade DECIMAL(5,2);
    DECLARE v_active_count INT DEFAULT 0;

    IF NEW.id_attempt IS NOT NULL THEN
        SELECT a.id_student_user, gs.id_subject, ass.id_subject, cg.id_subject, ass.max_grade
        INTO v_attempt_student, v_grade_sheet_subject, v_assessment_subject, v_block_subject, v_max_grade
        FROM attempt a
        JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
        JOIN assessment ass ON ass.id_assessment = a.id_assessment
        LEFT JOIN content_block cb ON cb.id_content_block = ass.id_content_block
        LEFT JOIN class_group cg ON cg.id_class_group = cb.id_class_group
        WHERE a.id_attempt = NEW.id_attempt;

        IF v_attempt_student <> NEW.id_user_student THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record Attempt must belong to the same Student';
        END IF;

        IF v_assessment_subject IS NOT NULL AND v_assessment_subject <> v_grade_sheet_subject THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record Attempt Assessment Subject must match Grade_Sheet Subject';
        END IF;

        IF v_block_subject IS NOT NULL AND v_block_subject <> v_grade_sheet_subject THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record Attempt Content_Block Subject must match Grade_Sheet Subject';
        END IF;

        IF v_max_grade IS NOT NULL AND NEW.value > v_max_grade THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record value cannot exceed the Assessment max_grade';
        END IF;
    END IF;

    IF NEW.state = 'active' THEN
        SELECT COUNT(*)
        INTO v_active_count
        FROM grade_record
        WHERE id_grade_sheet = NEW.id_grade_sheet
          AND id_user_student = NEW.id_user_student
          AND state = 'active'
          AND id_grade_record <> NEW.id_grade_record;

        IF v_active_count > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only one active Grade_Record is allowed per Grade_Sheet and Student';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_bgsc_validate$$
CREATE TRIGGER bi_bgsc_validate
BEFORE INSERT ON based_on_grade_sheet_certificate
FOR EACH ROW
BEGIN
    DECLARE v_course BIGINT UNSIGNED;
    DECLARE v_subject BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT c.id_course, gs.id_subject
    INTO v_course, v_subject
    FROM certificate c
    JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
    WHERE c.id_certificate = NEW.id_certificate;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = v_course
      AND id_subject = v_subject
      AND state <> 'archived';

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course must integrate the Subject of the referenced Grade_Sheet';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_message_validate$$
CREATE TRIGGER bi_message_validate
BEFORE INSERT ON message
FOR EACH ROW
BEGIN
    DECLARE v_channel BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    IF NEW.id_user_sender IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM participate_channel
        WHERE id_user = NEW.id_user_sender
          AND id_channel = NEW.id_channel
          AND state = 'active';

        IF v_exists = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Message sender must actively participate in the Channel';
        END IF;
    END IF;

    IF NEW.id_parent_message IS NOT NULL THEN
        SELECT id_channel
        INTO v_channel
        FROM message
        WHERE id_message = NEW.id_parent_message;

        IF v_channel <> NEW.id_channel THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Reply Message must belong to the same Channel as the parent Message';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_message_validate$$
CREATE TRIGGER bu_message_validate
BEFORE UPDATE ON message
FOR EACH ROW
BEGIN
    DECLARE v_channel BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    IF NEW.id_user_sender IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM participate_channel
        WHERE id_user = NEW.id_user_sender
          AND id_channel = NEW.id_channel
          AND state = 'active';

        IF v_exists = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Message sender must actively participate in the Channel';
        END IF;
    END IF;

    IF NEW.id_parent_message IS NOT NULL THEN
        SELECT id_channel
        INTO v_channel
        FROM message
        WHERE id_message = NEW.id_parent_message;

        IF v_channel <> NEW.id_channel THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Reply Message must belong to the same Channel as the parent Message';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_receive_message_validate$$
CREATE TRIGGER bi_receive_message_validate
BEFORE INSERT ON receive_message
FOR EACH ROW
BEGIN
    DECLARE v_email VARCHAR(160);
    DECLARE v_user_state VARCHAR(20);

    IF NEW.delivery_mode IN ('email', 'both') THEN
        SELECT email, state
        INTO v_email, v_user_state
        FROM user_account
        WHERE id_user = NEW.id_user;

        IF COALESCE(TRIM(v_email), '') = '' OR v_user_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Email delivery requires an active User with a valid email';
        END IF;
    END IF;

    IF NEW.state = 'delivered' AND NEW.delivered_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Delivered Receive_Message requires delivered_at';
    END IF;

    IF NEW.state = 'read' AND (NEW.delivered_at IS NULL OR NEW.read_at IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Read Receive_Message requires delivered_at and read_at';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_activity_log_validate$$
CREATE TRIGGER bi_activity_log_validate
BEFORE INSERT ON activity_log
FOR EACH ROW
BEGIN
    DECLARE v_session_user BIGINT UNSIGNED;

    IF NEW.id_session IS NOT NULL AND NEW.id_user IS NOT NULL THEN
        SELECT id_user
        INTO v_session_user
        FROM user_session
        WHERE id_session = NEW.id_session;

        IF v_session_user <> NEW.id_user THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Activity_Log Session must belong to the same User';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_deletion_request_validate$$
CREATE TRIGGER bi_deletion_request_validate
BEFORE INSERT ON deletion_request
FOR EACH ROW
BEGIN
    IF NEW.processed_at IS NOT NULL AND NEW.processor_admin_user_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Processed Deletion_Request requires processor_admin_user_id';
    END IF;

    IF NEW.state IN ('approved', 'rejected', 'completed')
       AND (NEW.processed_at IS NULL OR NEW.processor_admin_user_id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Final Deletion_Request state requires processed_at and processor_admin_user_id';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_deletion_request_validate$$
CREATE TRIGGER bu_deletion_request_validate
BEFORE UPDATE ON deletion_request
FOR EACH ROW
BEGIN
    IF NEW.processed_at IS NOT NULL AND NEW.processor_admin_user_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Processed Deletion_Request requires processor_admin_user_id';
    END IF;

    IF NEW.state IN ('approved', 'rejected', 'completed')
       AND (NEW.processed_at IS NULL OR NEW.processor_admin_user_id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Final Deletion_Request state requires processed_at and processor_admin_user_id';
    END IF;
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- Restricoes que devem ficar no Service (nao totalmente SQL):
-- - autorizacoes por perfil em operacoes de processo/aprovacao;
-- - coerencia cruzada de contexto (curso/disciplina/turma/bloco);
-- - limites temporais com regras de negocio avancadas;
-- - "pelo menos um" em relacoes opcionais do lado pai;
-- - regras de encaminhamento/destino de mensagens.
-- ---------------------------------------------------------
