-- GAPE - Base de dados relacional (MySQL 8+)
-- Fonte principal: documento "0. GAPE - ALL - V3"
-- NOTE: cross-entity application integrity constraints are enforced by the Service layer.

SET NAMES utf8mb4;
-- DatabaseConfig prepares bootstrap connections with the Europe/Lisbon offset.
-- Do not force UTC here: CURRENT_TIMESTAMP must follow the application time zone.

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
    KEY idx_user_account_state_name_email (state, name, email),
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
    context_type VARCHAR(30) NOT NULL DEFAULT 'GLOBAL',
    context_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id_admin_user, cod_permission, context_type, context_id),
    KEY idx_grant_administrator_permission (cod_permission),
    KEY idx_grant_administrator_context (context_type, context_id),
    CONSTRAINT fk_grant_administrator_admin
        FOREIGN KEY (id_admin_user) REFERENCES administrator_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_grant_administrator_permission
        FOREIGN KEY (cod_permission) REFERENCES permission (cod_permission)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_grant_administrator_context_type
        CHECK (context_type IN ('GLOBAL', 'ORGANIZATION', 'ORGANIC_UNIT', 'COURSE', 'SUBJECT', 'CLASS_GROUP'))
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
    acronym VARCHAR(30) NOT NULL,
    photo VARCHAR(255) NULL,
    type VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_organization),
    UNIQUE KEY uq_organization_name (name),
    UNIQUE KEY uq_organization_acronym (acronym),
    CONSTRAINT ck_organization_type
        CHECK (type IN ('educational_institution', 'training_company', 'company', 'other')),
    CONSTRAINT ck_organization_state
        CHECK (state IN ('active', 'inactive')),
    CONSTRAINT ck_organization_name_separator
        CHECK (LOCATE('|', name) = 0),
    CONSTRAINT ck_organization_acronym_separator
        CHECK (LOCATE('|', acronym) = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS organic_unit (
    id_organic_unit BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_organization BIGINT UNSIGNED NOT NULL,
    cod_organic_unit VARCHAR(30) NOT NULL,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NOT NULL,
    type VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    parent_organic_unit_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id_organic_unit),
    UNIQUE KEY uq_organic_unit_org_code (id_organization, cod_organic_unit),
    UNIQUE KEY uq_organic_unit_org_acronym (id_organization, acronym),
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
        CHECK (state IN ('active', 'inactive')),
    CONSTRAINT ck_organic_unit_name_separator
        CHECK (LOCATE('|', name) = 0),
    CONSTRAINT ck_organic_unit_acronym_separator
        CHECK (LOCATE('|', acronym) = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course (
    id_course BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_organization BIGINT UNSIGNED NOT NULL,
    id_organic_unit BIGINT UNSIGNED NULL,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NOT NULL,
    photo VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    ects DECIMAL(7,2) NOT NULL,
    certificate_max_grade DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    duration VARCHAR(40) NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'annual',
    type VARCHAR(40) NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_course),
    UNIQUE KEY uq_course_org_acronym (id_organization, acronym),
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
        CHECK (state IN ('active', 'inactive')),
    CONSTRAINT ck_course_ects
        CHECK (ects > 0),
    CONSTRAINT ck_course_certificate_max_grade
        CHECK (certificate_max_grade > 0),
    CONSTRAINT ck_course_duration_years
        CHECK (CAST(duration AS UNSIGNED) > 0 AND duration REGEXP '^[0-9]+$'),
    CONSTRAINT ck_course_frequency
        CHECK (frequency IN ('monthly', 'bimonthly', 'trimester', 'quadrimester', 'semester', 'annual')),
    CONSTRAINT ck_course_name_separator
        CHECK (LOCATE('|', name) = 0),
    CONSTRAINT ck_course_acronym_separator
        CHECK (LOCATE('|', acronym) = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_period_template (
    id_course_period_template BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_course BIGINT UNSIGNED NOT NULL,
    curricular_year INT NOT NULL,
    term VARCHAR(20) NOT NULL,
    starts_month TINYINT UNSIGNED NOT NULL,
    starts_day TINYINT UNSIGNED NOT NULL,
    ends_month TINYINT UNSIGNED NOT NULL,
    ends_day TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_course_period_template),
    UNIQUE KEY uq_course_period_template_position (id_course, curricular_year, term),
    KEY idx_course_period_template_course (id_course),
    CONSTRAINT fk_course_period_template_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_course_period_template_year
        CHECK (curricular_year > 0),
    CONSTRAINT ck_course_period_template_term
        CHECK (term IN (
            'annual',
            'semester_1', 'semester_2',
            'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
            'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
            'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
        )),
    CONSTRAINT ck_course_period_template_month_day
        CHECK (
            starts_month BETWEEN 1 AND 12
            AND ends_month BETWEEN 1 AND 12
            AND starts_day BETWEEN 1 AND 31
            AND ends_day BETWEEN 1 AND 31
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_occurrence (
    id_course_occurrence BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_course BIGINT UNSIGNED NOT NULL,
    reference_year INT NOT NULL,
    label VARCHAR(220) NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_course_occurrence),
    UNIQUE KEY uq_course_occurrence_label (id_course, label),
    UNIQUE KEY uq_course_occurrence_reference_year (id_course, reference_year),
    KEY idx_course_occurrence_course (id_course),
    KEY idx_course_occurrence_state (state),
    CONSTRAINT fk_course_occurrence_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_course_occurrence_dates
        CHECK (ends_at >= starts_at),
    CONSTRAINT ck_course_occurrence_reference_year
        CHECK (reference_year BETWEEN 1900 AND 9998),
    CONSTRAINT ck_course_occurrence_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'cancelled')),
    CONSTRAINT ck_course_occurrence_label_separator
        CHECK (LOCATE('|', label) = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS course_occurrence_period (
    id_course_occurrence_period BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_course_occurrence BIGINT UNSIGNED NOT NULL,
    curricular_year INT NOT NULL,
    term VARCHAR(20) NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_course_occurrence_period),
    UNIQUE KEY uq_course_occurrence_period_position (id_course_occurrence, curricular_year, term),
    KEY idx_course_occurrence_period_occurrence (id_course_occurrence),
    KEY idx_course_occurrence_period_state (state),
    CONSTRAINT fk_course_occurrence_period_occurrence
        FOREIGN KEY (id_course_occurrence) REFERENCES course_occurrence (id_course_occurrence)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_course_occurrence_period_year
        CHECK (curricular_year > 0),
    CONSTRAINT ck_course_occurrence_period_term
        CHECK (term IN (
            'annual',
            'semester_1', 'semester_2',
            'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
            'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
            'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
        )),
    CONSTRAINT ck_course_occurrence_period_dates
        CHECK (ends_at >= starts_at),
    CONSTRAINT ck_course_occurrence_period_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'cancelled'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS subject (
    id_subject BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_organization BIGINT UNSIGNED NOT NULL,
    id_organic_unit BIGINT UNSIGNED NULL,
    name VARCHAR(160) NOT NULL,
    acronym VARCHAR(30) NOT NULL,
    photo VARCHAR(255) NULL,
    description VARCHAR(500) NULL,
    ects DECIMAL(7,2) NOT NULL,
    final_grade_max DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    workload_hours INT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_subject),
    UNIQUE KEY uq_subject_org_name (id_organization, name),
    UNIQUE KEY uq_subject_org_acronym (id_organization, acronym),
    KEY idx_subject_org (id_organization),
    KEY idx_subject_organic_unit (id_organic_unit),
    KEY idx_subject_state (state),
    CONSTRAINT fk_subject_org
        FOREIGN KEY (id_organization) REFERENCES organization (id_organization)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_subject_organic_unit
        FOREIGN KEY (id_organic_unit) REFERENCES organic_unit (id_organic_unit)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_subject_ects
        CHECK (ects > 0),
    CONSTRAINT ck_subject_final_grade_max
        CHECK (final_grade_max > 0),
    CONSTRAINT ck_subject_workload_hours
        CHECK (workload_hours IS NULL OR workload_hours >= 0),
    CONSTRAINT ck_subject_state
        CHECK (state IN ('active', 'inactive')),
    CONSTRAINT ck_subject_name_separator
        CHECK (LOCATE('|', name) = 0),
    CONSTRAINT ck_subject_acronym_separator
        CHECK (LOCATE('|', acronym) = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS class_group (
    id_class_group BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_subject BIGINT UNSIGNED NOT NULL,
    id_course BIGINT UNSIGNED NOT NULL,
    id_course_occurrence BIGINT UNSIGNED NOT NULL,
    id_course_occurrence_period BIGINT UNSIGNED NOT NULL,
    cod_class_group VARCHAR(30) NOT NULL,
    modality VARCHAR(30) NOT NULL,
    state VARCHAR(20) NOT NULL,
    min_students INT NOT NULL,
    max_students INT NOT NULL,
    starts_at DATE NOT NULL,
    ends_at DATE NOT NULL,
    shift VARCHAR(30) NOT NULL,
    show_content_thumbnails BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_class_group),
    UNIQUE KEY uq_class_group_occurrence_subject_code (id_course_occurrence, id_subject, cod_class_group),
    KEY idx_class_group_subject (id_subject),
    KEY idx_class_group_course (id_course),
    KEY idx_class_group_occurrence (id_course_occurrence),
    KEY idx_class_group_occurrence_period (id_course_occurrence_period),
    KEY idx_class_group_state (state),
    CONSTRAINT fk_class_group_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_class_group_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_class_group_course_occurrence
        FOREIGN KEY (id_course_occurrence) REFERENCES course_occurrence (id_course_occurrence)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_class_group_course_occurrence_period
        FOREIGN KEY (id_course_occurrence_period) REFERENCES course_occurrence_period (id_course_occurrence_period)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_class_group_modality
        CHECK (modality IN ('onsite', 'online', 'hybrid')),
    CONSTRAINT ck_class_group_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed')),
    CONSTRAINT ck_class_group_scheduled_start
        CHECK (state <> 'scheduled' OR starts_at IS NOT NULL),
    CONSTRAINT ck_class_group_shift
        CHECK (shift IN ('morning', 'afternoon', 'evening', 'mixed')),
    CONSTRAINT ck_class_group_students_range
        CHECK (
            min_students > 0
            AND max_students > min_students
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
    state VARCHAR(30) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_content_block),
    UNIQUE KEY uq_content_block_group_code (id_class_group, cod_content_block),
    UNIQUE KEY uq_content_block_order (id_class_group, order_no),
    KEY idx_content_block_group_order (id_class_group, order_no),
    CONSTRAINT fk_content_block_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_content_block_order
        CHECK (order_no > 0),
    CONSTRAINT ck_content_block_state
        CHECK (state IN ('active', 'inactive')),
    CONSTRAINT ck_content_block_updated
        CHECK (updated_at IS NULL OR updated_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS integrate_subject (
    id_course BIGINT UNSIGNED NOT NULL,
    id_subject BIGINT UNSIGNED NOT NULL,
    curricular_year INT NULL,
    term VARCHAR(20) NULL,
    mandatory BOOLEAN NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'active',
    ended_at DATE NULL,
    PRIMARY KEY (id_course, id_subject),
    KEY idx_integrate_subject_subject (id_subject),
    KEY idx_integrate_subject_state (state),
    CONSTRAINT fk_integrate_subject_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_integrate_subject_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_integrate_subject_year
        CHECK (curricular_year IS NULL OR curricular_year > 0),
    CONSTRAINT ck_integrate_subject_term
        CHECK (term IS NULL OR term IN (
            'annual',
            'semester_1', 'semester_2',
            'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
            'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
            'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
        )),
    CONSTRAINT ck_integrate_subject_curricular_position
        CHECK (
            (curricular_year IS NULL AND term IS NULL)
            OR (curricular_year IS NOT NULL AND term IS NOT NULL)
        ),
    CONSTRAINT ck_integrate_subject_state
        CHECK (state IN ('active', 'historical')),
    CONSTRAINT ck_integrate_subject_ended_at
        CHECK ((state = 'active' AND ended_at IS NULL) OR (state = 'historical' AND ended_at IS NOT NULL))
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
        CHECK (state IN ('active', 'inactive'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS coordinate_subject (
    id_coordinator_user BIGINT UNSIGNED NOT NULL,
    id_subject BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_coordinator_user, id_subject),
    KEY idx_coordinate_subject_subject (id_subject),
    CONSTRAINT fk_coordinate_subject_coordinator
        FOREIGN KEY (id_coordinator_user) REFERENCES coordinator_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_coordinate_subject_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_coordinate_subject_state
        CHECK (state IN ('active', 'inactive'))
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
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_teach_class_group_state
        CHECK (state IN ('active', 'inactive'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enroll_course (
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_course BIGINT UNSIGNED NOT NULL,
    id_course_occurrence BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL DEFAULT '1000-01-01',
    end_date DATE NOT NULL DEFAULT '1000-01-01',
    PRIMARY KEY (id_student_user, id_course_occurrence),
    KEY idx_enroll_course_course (id_course),
    KEY idx_enroll_course_occurrence (id_course_occurrence),
    CONSTRAINT fk_enroll_course_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enroll_course_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enroll_course_occurrence
        FOREIGN KEY (id_course_occurrence) REFERENCES course_occurrence (id_course_occurrence)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_enroll_course_dates
        CHECK (end_date >= start_date),
    CONSTRAINT ck_enroll_course_state
        CHECK (state IN ('active', 'inactive', 'completed', 'withdrawn'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enroll_class_group (
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL DEFAULT '1000-01-01',
    end_date DATE NOT NULL DEFAULT '1000-01-01',
    PRIMARY KEY (id_student_user, id_class_group),
    KEY idx_enroll_class_group_class_group (id_class_group),
    CONSTRAINT fk_enroll_class_group_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_enroll_class_group_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_enroll_class_group_dates
        CHECK (end_date >= start_date),
    CONSTRAINT ck_enroll_class_group_state
        CHECK (state IN ('pending', 'active', 'inactive', 'rejected', 'completed', 'withdrawn'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS class_group_enrollment_policy (
    id_class_group BIGINT UNSIGNED NOT NULL,
    approval_mode VARCHAR(30) NOT NULL DEFAULT 'manual',
    PRIMARY KEY (id_class_group),
    CONSTRAINT fk_class_group_enrollment_policy_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_class_group_enrollment_policy_mode
        CHECK (approval_mode IN ('manual', 'auto_approve'))
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
        CHECK (format IN ('text', 'image', 'video', 'audio', 'pdf', 'archive', 'url', 'scorm', 'xapi', 'presentation', 'embed', 'other')),
    CONSTRAINT ck_content_item_state
        CHECK (state IN ('draft', 'active', 'inactive')),
    CONSTRAINT ck_content_item_updated
        CHECK (updated_at IS NULL OR updated_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS content_file (
    id_content_file BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_content_item BIGINT UNSIGNED NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    original_mime_type VARCHAR(120) NOT NULL,
    final_mime_type VARCHAR(120) NOT NULL,
    original_bytes BIGINT UNSIGNED NOT NULL,
    final_bytes BIGINT UNSIGNED NOT NULL,
    sha256 CHAR(64) NULL,
    original_path VARCHAR(500) NULL,
    final_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500) NULL,
    duration_seconds INT UNSIGNED NULL,
    width INT UNSIGNED NULL,
    height INT UNSIGNED NULL,
    page_count INT UNSIGNED NULL,
    processing_state VARCHAR(20) NOT NULL,
    processing_error VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    processed_at DATETIME NULL,
    PRIMARY KEY (id_content_file),
    UNIQUE KEY uq_content_file_item_final_path (id_content_item, final_path),
    KEY idx_content_file_item (id_content_item),
    KEY idx_content_file_processing_state (processing_state),
    CONSTRAINT fk_content_file_item
        FOREIGN KEY (id_content_item) REFERENCES content_item (id_content_item)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_content_file_sizes
        CHECK (original_bytes > 0 AND final_bytes > 0),
    CONSTRAINT ck_content_file_dimensions
        CHECK ((width IS NULL OR width > 0) AND (height IS NULL OR height > 0)),
    CONSTRAINT ck_content_file_processing_state
        CHECK (processing_state IN ('processing', 'ready', 'failed')),
    CONSTRAINT ck_content_file_processed
        CHECK (processed_at IS NULL OR processed_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS physical_room (
    cod_physical_room VARCHAR(40) NOT NULL,
    id_organization BIGINT UNSIGNED NOT NULL,
    id_organic_unit BIGINT UNSIGNED NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    capacity INT NOT NULL,
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
        CHECK (capacity > 0),
    CONSTRAINT ck_physical_room_state
        CHECK (state IN ('active', 'inactive', 'unavailable'))
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
    order_no INT NULL,
    PRIMARY KEY (id_lesson),
    KEY idx_lesson_class_group (id_class_group),
    KEY idx_lesson_content_block (id_content_block),
    KEY idx_lesson_block_order (id_content_block, order_no),
    KEY idx_lesson_room (cod_physical_room),
    KEY idx_lesson_room_state_dates (cod_physical_room, state, starts_at, ends_at),
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
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed', 'cancelled')),
    CONSTRAINT ck_lesson_scheduled_start
        CHECK (state <> 'scheduled' OR starts_at IS NOT NULL),
    CONSTRAINT ck_lesson_dates
        CHECK (starts_at IS NULL OR ends_at IS NULL OR ends_at > starts_at),
    CONSTRAINT ck_lesson_order_no
        CHECK (order_no IS NULL OR order_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assessment (
    id_assessment BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_subject BIGINT UNSIGNED NULL,
    id_content_block BIGINT UNSIGNED NULL,
    cod_physical_room VARCHAR(40) NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    type VARCHAR(30) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    correction_mode VARCHAR(30) NOT NULL,
    max_grade DECIMAL(5,2) NOT NULL,
    passing_grade DECIMAL(5,2) NOT NULL,
    final_grade_weight DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    attempts_limit INT NULL,
    enrollment_mode VARCHAR(30) NOT NULL DEFAULT 'auto_approve',
    state VARCHAR(20) NOT NULL,
    available_from DATETIME NOT NULL,
    available_until DATETIME NOT NULL,
    order_no INT NULL,
    PRIMARY KEY (id_assessment),
    KEY idx_assessment_subject (id_subject),
    KEY idx_assessment_content_block (id_content_block),
    KEY idx_assessment_room (cod_physical_room),
    KEY idx_assessment_block_order (id_content_block, order_no),
    KEY idx_assessment_state (state),
    CONSTRAINT fk_assessment_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_assessment_content_block
        FOREIGN KEY (id_content_block) REFERENCES content_block (id_content_block)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_assessment_physical_room
        FOREIGN KEY (cod_physical_room) REFERENCES physical_room (cod_physical_room)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_assessment_type
        CHECK (type IN ('form', 'test', 'exam')),
    CONSTRAINT ck_assessment_mode
        CHECK (mode IN ('online', 'onsite')),
    CONSTRAINT ck_assessment_correction_mode
        CHECK (correction_mode IN ('automatic', 'mixed', 'manual')),
    CONSTRAINT ck_assessment_enrollment_mode
        CHECK (enrollment_mode IN ('manual', 'auto_approve')),
    CONSTRAINT ck_assessment_state
        CHECK (state IN ('draft', 'scheduled', 'active', 'completed')),
    CONSTRAINT ck_assessment_grades
        CHECK (max_grade >= 0 AND passing_grade >= 0 AND passing_grade <= max_grade),
    CONSTRAINT ck_assessment_final_grade_weight
        CHECK (final_grade_weight >= 0 AND final_grade_weight <= 100),
    CONSTRAINT ck_assessment_attempts_limit
        CHECK (attempts_limit IS NULL OR attempts_limit > 0),
    CONSTRAINT ck_assessment_scheduled_start
        CHECK (state <> 'scheduled' OR available_from IS NOT NULL),
    CONSTRAINT ck_assessment_availability
        CHECK (
            available_from IS NULL
            OR available_until IS NULL
            OR available_until >= available_from
        ),
    CONSTRAINT ck_assessment_order_no
        CHECK (order_no IS NULL OR order_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assessment_class_group (
    id_assessment BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_assessment, id_class_group),
    KEY idx_assessment_class_group_group (id_class_group),
    CONSTRAINT fk_assessment_class_group_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_assessment_class_group_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE RESTRICT
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
    PRIMARY KEY (id_question),
    UNIQUE KEY uq_question_assessment_code (id_assessment, cod_question),
    UNIQUE KEY uq_question_assessment_order (id_assessment, order_no),
    KEY idx_question_type (type),
    CONSTRAINT fk_question_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_question_type
        CHECK (type IN ('single_choice', 'multiple_choice', 'short_text', 'paragraph', 'file_upload', 'rating')),
    CONSTRAINT ck_question_order
        CHECK (order_no > 0),
    CONSTRAINT ck_question_score
        CHECK (score >= 0.10),
    CONSTRAINT ck_question_rating_expected_answer
        CHECK (type <> 'rating' OR (expected_answer IS NOT NULL AND TRIM(expected_answer) <> ''))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS enroll_assessment (
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_assessment BIGINT UNSIGNED NOT NULL,
    state VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL DEFAULT '1000-01-01',
    end_date DATE NOT NULL DEFAULT '1000-01-01',
    PRIMARY KEY (id_student_user, id_assessment),
    KEY idx_enroll_assessment_assessment (id_assessment),
    KEY idx_enroll_assessment_state (state),
    CONSTRAINT fk_enroll_assessment_student
        FOREIGN KEY (id_student_user) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_enroll_assessment_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_enroll_assessment_dates
        CHECK (end_date >= start_date),
    CONSTRAINT ck_enroll_assessment_state
        CHECK (state IN ('pending', 'active', 'inactive', 'rejected', 'completed', 'withdrawn'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_option (
    id_option BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_question BIGINT UNSIGNED NOT NULL,
    order_no INT NOT NULL,
    text VARCHAR(300) NOT NULL,
    correct_flag BOOLEAN NULL,
    PRIMARY KEY (id_option),
    UNIQUE KEY uq_question_option_order (id_question, order_no),
    CONSTRAINT fk_question_option_question
        FOREIGN KEY (id_question) REFERENCES question (id_question)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_question_option_order
        CHECK (order_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attempt (
    id_attempt BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_student_user BIGINT UNSIGNED NOT NULL,
    id_assessment BIGINT UNSIGNED NOT NULL,
    attempt_number INT NOT NULL,
    score DECIMAL(7,3) NULL,
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
    CONSTRAINT ck_attempt_score_requires_correction
        CHECK (score IS NULL OR state = 'corrected'),
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
    score DECIMAL(7,3) NULL,
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
        CHECK (state IN ('draft', 'active', 'inactive', 'cancelled', 'completed'))
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

CREATE TABLE IF NOT EXISTS learning_event (
    id_learning_event BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_type VARCHAR(40) NOT NULL,
    source_key VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    category VARCHAR(40) NOT NULL,
    category_label VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500) NULL,
    context_label VARCHAR(180) NOT NULL,
    context_title VARCHAR(500) NOT NULL,
    id_class_group BIGINT UNSIGNED NULL,
    id_course BIGINT UNSIGNED NULL,
    id_subject BIGINT UNSIGNED NULL,
    id_student_user BIGINT UNSIGNED NULL,
    detail_href VARCHAR(300) NOT NULL,
    occurred_at DATETIME NOT NULL,
    state_label VARCHAR(80) NOT NULL,
    state_value VARCHAR(60) NOT NULL,
    icon_class VARCHAR(80) NOT NULL,
    badge_class VARCHAR(120) NOT NULL,
    state_badge_class VARCHAR(120) NOT NULL,
    visibility_state VARCHAR(20) NOT NULL DEFAULT 'visible',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_learning_event),
    UNIQUE KEY uq_learning_event_source (source_type, source_key, event_type),
    KEY idx_learning_event_category (category),
    KEY idx_learning_event_occurred_at (occurred_at),
    KEY idx_learning_event_class_group (id_class_group),
    KEY idx_learning_event_course (id_course),
    KEY idx_learning_event_subject (id_subject),
    KEY idx_learning_event_student (id_student_user),
    CONSTRAINT fk_learning_event_class_group
        FOREIGN KEY (id_class_group) REFERENCES class_group (id_class_group)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_learning_event_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_learning_event_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_learning_event_student
        FOREIGN KEY (id_student_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_learning_event_source_type
        CHECK (source_type IN (
            'lesson', 'assessment', 'attendance', 'absence_justification',
            'grade_sheet', 'certificate', 'enroll_class_group',
            'enroll_course', 'class_group', 'attempt',
            'teach_class_group', 'coordinate_subject', 'manage_organization',
            'deletion_request', 'user_account'
        )),
    CONSTRAINT ck_learning_event_category
        CHECK (category IN (
            'lessons', 'assessments', 'attendance', 'absence_justifications',
            'grade_sheets', 'certificates', 'class_group_enrollments',
            'course_enrollments', 'class_groups',
            'attempts', 'teacher_assignments',
            'subject_coordination', 'organization_management',
            'deletion_requests', 'users'
        )),
    CONSTRAINT ck_learning_event_visibility
        CHECK (visibility_state IN ('visible', 'hidden'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS learning_event_read (
    id_learning_event BIGINT UNSIGNED NOT NULL,
    id_user BIGINT UNSIGNED NOT NULL,
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_learning_event, id_user),
    KEY idx_learning_event_read_user (id_user, read_at),
    CONSTRAINT fk_learning_event_read_event
        FOREIGN KEY (id_learning_event) REFERENCES learning_event (id_learning_event)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_learning_event_read_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
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
    active_record_key TINYINT GENERATED ALWAYS AS (
        CASE WHEN state = 'active' THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id_attendance_record),
    UNIQUE KEY uq_attendance_active_lesson_student (id_lesson, id_user_student, active_record_key),
    KEY idx_attendance_student (id_user_student),
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
        CHECK (state IN ('active', 'corrected', 'cancelled')),
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
    id_course_occurrence BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    type VARCHAR(40) NOT NULL,
    max_grade DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    passing_grade DECIMAL(5,2) NOT NULL DEFAULT 9.50,
    weight_alert VARCHAR(255) NULL,
    released_at DATETIME NULL,
    publication_explanation VARCHAR(1000) NULL,
    state VARCHAR(20) NOT NULL,
    scope VARCHAR(30) NOT NULL DEFAULT 'class_group',
    -- Nullable unique key for the consolidated subject-occurrence sheet.
    -- Keeping this explicit instead of generated is compatible with the
    -- MySQL version used by the project while the validation triggers below
    -- keep it exactly aligned with scope and id_course_occurrence.
    subject_occurrence_aggregate_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id_grade_sheet),
    KEY idx_grade_sheet_subject (id_subject),
    KEY idx_grade_sheet_occurrence (id_course_occurrence),
    KEY idx_grade_sheet_state (state),
    UNIQUE KEY uq_grade_sheet_subject_occurrence_aggregate (
        id_subject, subject_occurrence_aggregate_id
    ),
    CONSTRAINT fk_grade_sheet_subject
        FOREIGN KEY (id_subject) REFERENCES subject (id_subject)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_grade_sheet_course_occurrence
        FOREIGN KEY (id_course_occurrence) REFERENCES course_occurrence (id_course_occurrence)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_grade_sheet_type
        CHECK (type IN ('final', 'continuous_assessment', 'exam', 'partial', 'other')),
    CONSTRAINT ck_grade_sheet_state
        CHECK (state IN ('draft', 'published', 'closed', 'inactive')),
    CONSTRAINT ck_grade_sheet_scope
        CHECK (scope IN ('class_group', 'subject_occurrence')),
    CONSTRAINT ck_grade_sheet_scale
        CHECK (max_grade > 0 AND passing_grade >= 0 AND passing_grade <= max_grade)
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
    weight DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (id_grade_sheet, id_assessment),
    KEY idx_based_on_assessment_assessment (id_assessment),
    CONSTRAINT fk_based_on_assessment_grade_sheet
        FOREIGN KEY (id_grade_sheet) REFERENCES grade_sheet (id_grade_sheet)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_based_on_assessment_assessment
        FOREIGN KEY (id_assessment) REFERENCES assessment (id_assessment)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_based_on_assessment_weight
        CHECK (weight >= 0 AND weight <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS grade_record (
    id_grade_record BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    id_user_student BIGINT UNSIGNED NOT NULL,
    id_attempt BIGINT UNSIGNED NULL,
    cod_grade_record VARCHAR(30) NOT NULL,
    value DECIMAL(5,2) NOT NULL,
    result VARCHAR(20) NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'published',
    recorded_at DATETIME NOT NULL,
    notes VARCHAR(500) NULL,
    active_student_user_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id_grade_record),
    UNIQUE KEY uq_grade_record_sheet_code (id_grade_sheet, cod_grade_record),
    UNIQUE KEY uq_grade_record_sheet_active_student (id_grade_sheet, active_student_user_id),
    UNIQUE KEY uq_grade_record_attempt (id_attempt),
    KEY idx_grade_record_student (id_user_student),
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
        CHECK (result IN ('approved', 'failed', 'pending', 'absent')),
    CONSTRAINT ck_grade_record_state
        CHECK (state IN ('draft', 'published', 'corrected', 'inactive')),
    CONSTRAINT ck_grade_record_value
        CHECK (value >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS certificate (
    id_certificate BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_course BIGINT UNSIGNED NOT NULL,
    id_course_occurrence BIGINT UNSIGNED NOT NULL,
    id_user_student BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    notes VARCHAR(500) NULL,
    type VARCHAR(40) NOT NULL,
    template VARCHAR(255) NULL,
    validation_code VARCHAR(80) NULL,
    issued_at DATETIME NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'draft',
    final_grade DECIMAL(5,2) NULL,
    PRIMARY KEY (id_certificate),
    UNIQUE KEY uq_certificate_occurrence_student (id_course_occurrence, id_user_student),
    UNIQUE KEY uq_certificate_validation_code (validation_code),
    KEY idx_certificate_course (id_course),
    KEY idx_certificate_occurrence (id_course_occurrence),
    KEY idx_certificate_student (id_user_student),
    CONSTRAINT fk_certificate_course
        FOREIGN KEY (id_course) REFERENCES course (id_course)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_certificate_course_occurrence
        FOREIGN KEY (id_course_occurrence) REFERENCES course_occurrence (id_course_occurrence)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_certificate_student
        FOREIGN KEY (id_user_student) REFERENCES student_profile (id_user)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_certificate_type
        CHECK (type IN ('completion', 'attendance', 'qualification', 'other')),
    CONSTRAINT ck_certificate_state
        CHECK (state IN ('draft', 'active', 'issued')),
    CONSTRAINT ck_certificate_issued_fields
        CHECK (
            (state = 'issued'
                AND validation_code IS NOT NULL
                AND issued_at IS NOT NULL
                AND final_grade IS NOT NULL)
            OR (state IN ('draft', 'active')
                AND validation_code IS NULL
                AND issued_at IS NULL
                AND final_grade IS NULL)
        )
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
    KEY idx_channel_state (state),
    KEY idx_channel_state_type_visibility (state, type, visibility, id_channel)
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
    KEY idx_participate_user_state_channel (id_user, state, id_channel),
    KEY idx_participate_channel_state_user (id_channel, state, id_user),
    CONSTRAINT fk_participate_channel_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_participate_channel_channel
        FOREIGN KEY (id_channel) REFERENCES channel (id_channel)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS direct_message_channel (
    id_user_low BIGINT UNSIGNED NOT NULL,
    id_user_high BIGINT UNSIGNED NOT NULL,
    id_channel BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_user_low, id_user_high),
    UNIQUE KEY uq_direct_message_channel_channel (id_channel),
    CONSTRAINT fk_direct_message_channel_low_user
        FOREIGN KEY (id_user_low) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_direct_message_channel_high_user
        FOREIGN KEY (id_user_high) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_direct_message_channel_channel
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
    KEY idx_message_channel_state_type_time (id_channel, state, type, sent_at, created_at, id_message),
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
        CHECK (state IN ('draft', 'scheduled', 'sent', 'active', 'edited', 'deleted', 'cancelled')),
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
    state VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_user, id_message),
    KEY idx_receive_message_message (id_message),
    KEY idx_receive_message_message_user_delivery (id_message, id_user, delivered_at, read_at, state),
    KEY idx_receive_message_user_state_delivery (id_user, state, delivered_at, read_at, id_message),
    CONSTRAINT fk_receive_message_user
        FOREIGN KEY (id_user) REFERENCES user_account (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_receive_message_message
        FOREIGN KEY (id_message) REFERENCES message (id_message)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_receive_message_state
        CHECK (state IN ('pending', 'delivered', 'read')),
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

DROP TRIGGER IF EXISTS bu_user_account_manage_all_guard$$
CREATE TRIGGER bu_user_account_manage_all_guard
BEFORE UPDATE ON user_account
FOR EACH ROW
BEGIN
    DECLARE v_is_global_manage_all INT DEFAULT 0;
    DECLARE v_other_global_manage_all INT DEFAULT 0;

    IF OLD.state = 'active' AND NEW.state <> 'active' THEN
        SELECT COUNT(*)
        INTO v_is_global_manage_all
        FROM grant_administrator ga
        JOIN permission p ON p.cod_permission = ga.cod_permission
        WHERE ga.id_admin_user = OLD.id_user
          AND ga.cod_permission = 'MANAGE_ALL'
          AND ga.context_type = 'GLOBAL'
          AND ga.context_id = 0
          AND p.state = 'active';

        IF v_is_global_manage_all > 0 THEN
            SELECT COUNT(*)
            INTO v_other_global_manage_all
            FROM grant_administrator ga
            JOIN administrator_profile ap ON ap.id_user = ga.id_admin_user
            JOIN user_account u ON u.id_user = ap.id_user
            JOIN permission p ON p.cod_permission = ga.cod_permission
            WHERE ga.id_admin_user <> OLD.id_user
              AND ga.cod_permission = 'MANAGE_ALL'
              AND ga.context_type = 'GLOBAL'
              AND ga.context_id = 0
              AND u.state = 'active'
              AND p.state = 'active';

            IF v_other_global_manage_all = 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User state change would remove the last MANAGE_ALL Administrator';
            END IF;
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_user_account_manage_all_guard$$
CREATE TRIGGER bd_user_account_manage_all_guard
BEFORE DELETE ON user_account
FOR EACH ROW
BEGIN
    DECLARE v_is_global_manage_all INT DEFAULT 0;
    DECLARE v_other_global_manage_all INT DEFAULT 0;

    IF OLD.state = 'active' THEN
        SELECT COUNT(*)
        INTO v_is_global_manage_all
        FROM grant_administrator ga
        JOIN permission p ON p.cod_permission = ga.cod_permission
        WHERE ga.id_admin_user = OLD.id_user
          AND ga.cod_permission = 'MANAGE_ALL'
          AND ga.context_type = 'GLOBAL'
          AND ga.context_id = 0
          AND p.state = 'active';

        IF v_is_global_manage_all > 0 THEN
            SELECT COUNT(*)
            INTO v_other_global_manage_all
            FROM grant_administrator ga
            JOIN administrator_profile ap ON ap.id_user = ga.id_admin_user
            JOIN user_account u ON u.id_user = ap.id_user
            JOIN permission p ON p.cod_permission = ga.cod_permission
            WHERE ga.id_admin_user <> OLD.id_user
              AND ga.cod_permission = 'MANAGE_ALL'
              AND ga.context_type = 'GLOBAL'
              AND ga.context_id = 0
              AND u.state = 'active'
              AND p.state = 'active';

            IF v_other_global_manage_all = 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'User deletion would remove the last MANAGE_ALL Administrator';
            END IF;
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_administrator_profile_manage_all_guard$$
CREATE TRIGGER bd_administrator_profile_manage_all_guard
BEFORE DELETE ON administrator_profile
FOR EACH ROW
BEGIN
    DECLARE v_user_state VARCHAR(20);
    DECLARE v_is_global_manage_all INT DEFAULT 0;
    DECLARE v_other_global_manage_all INT DEFAULT 0;

    SELECT state
    INTO v_user_state
    FROM user_account
    WHERE id_user = OLD.id_user;

    IF v_user_state = 'active' THEN
        SELECT COUNT(*)
        INTO v_is_global_manage_all
        FROM grant_administrator ga
        JOIN permission p ON p.cod_permission = ga.cod_permission
        WHERE ga.id_admin_user = OLD.id_user
          AND ga.cod_permission = 'MANAGE_ALL'
          AND ga.context_type = 'GLOBAL'
          AND ga.context_id = 0
          AND p.state = 'active';

        IF v_is_global_manage_all > 0 THEN
            SELECT COUNT(*)
            INTO v_other_global_manage_all
            FROM grant_administrator ga
            JOIN administrator_profile ap ON ap.id_user = ga.id_admin_user
            JOIN user_account u ON u.id_user = ap.id_user
            JOIN permission p ON p.cod_permission = ga.cod_permission
            WHERE ga.id_admin_user <> OLD.id_user
              AND ga.cod_permission = 'MANAGE_ALL'
              AND ga.context_type = 'GLOBAL'
              AND ga.context_id = 0
              AND u.state = 'active'
              AND p.state = 'active';

            IF v_other_global_manage_all = 0 THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Administrator profile deletion would remove the last MANAGE_ALL Administrator';
            END IF;
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_grant_administrator_manage_all_guard$$
CREATE TRIGGER bd_grant_administrator_manage_all_guard
BEFORE DELETE ON grant_administrator
FOR EACH ROW
BEGIN
    DECLARE v_user_state VARCHAR(20);
    DECLARE v_permission_state VARCHAR(20);
    DECLARE v_other_global_manage_all INT DEFAULT 0;

    SELECT u.state
    INTO v_user_state
    FROM administrator_profile ap
    JOIN user_account u ON u.id_user = ap.id_user
    WHERE ap.id_user = OLD.id_admin_user;

    SELECT state
    INTO v_permission_state
    FROM permission
    WHERE cod_permission = OLD.cod_permission;

    IF OLD.cod_permission = 'MANAGE_ALL'
       AND OLD.context_type = 'GLOBAL'
       AND OLD.context_id = 0
       AND v_user_state = 'active'
       AND v_permission_state = 'active' THEN
        SELECT COUNT(*)
        INTO v_other_global_manage_all
        FROM grant_administrator ga
        JOIN administrator_profile ap ON ap.id_user = ga.id_admin_user
        JOIN user_account u ON u.id_user = ap.id_user
        JOIN permission p ON p.cod_permission = ga.cod_permission
        WHERE ga.id_admin_user <> OLD.id_admin_user
          AND ga.cod_permission = 'MANAGE_ALL'
          AND ga.context_type = 'GLOBAL'
          AND ga.context_id = 0
          AND u.state = 'active'
          AND p.state = 'active';

        IF v_other_global_manage_all = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Permission deletion would remove the last MANAGE_ALL Administrator';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_grant_administrator_manage_all_guard$$
CREATE TRIGGER bu_grant_administrator_manage_all_guard
BEFORE UPDATE ON grant_administrator
FOR EACH ROW
BEGIN
    DECLARE v_user_state VARCHAR(20);
    DECLARE v_permission_state VARCHAR(20);
    DECLARE v_other_global_manage_all INT DEFAULT 0;

    SELECT u.state
    INTO v_user_state
    FROM administrator_profile ap
    JOIN user_account u ON u.id_user = ap.id_user
    WHERE ap.id_user = OLD.id_admin_user;

    SELECT state
    INTO v_permission_state
    FROM permission
    WHERE cod_permission = OLD.cod_permission;

    IF OLD.cod_permission = 'MANAGE_ALL'
       AND OLD.context_type = 'GLOBAL'
       AND OLD.context_id = 0
       AND v_user_state = 'active'
       AND v_permission_state = 'active'
       AND NOT (
           NEW.id_admin_user = OLD.id_admin_user
           AND NEW.cod_permission = OLD.cod_permission
           AND NEW.context_type = OLD.context_type
           AND NEW.context_id = OLD.context_id
       ) THEN
        SELECT COUNT(*)
        INTO v_other_global_manage_all
        FROM grant_administrator ga
        JOIN administrator_profile ap ON ap.id_user = ga.id_admin_user
        JOIN user_account u ON u.id_user = ap.id_user
        JOIN permission p ON p.cod_permission = ga.cod_permission
        WHERE ga.id_admin_user <> OLD.id_admin_user
          AND ga.cod_permission = 'MANAGE_ALL'
          AND ga.context_type = 'GLOBAL'
          AND ga.context_id = 0
          AND u.state = 'active'
          AND p.state = 'active';

        IF v_other_global_manage_all = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Permission update would remove the last MANAGE_ALL Administrator';
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

DROP TRIGGER IF EXISTS bi_course_period_template_validate$$
CREATE TRIGGER bi_course_period_template_validate
BEFORE INSERT ON course_period_template
FOR EACH ROW
BEGIN
    DECLARE v_duration INT;
    DECLARE v_frequency VARCHAR(20);
    DECLARE v_start_date DATE;
    DECLARE v_end_date DATE;

    SELECT CAST(duration AS UNSIGNED), frequency
    INTO v_duration, v_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period year must be inside Course duration';
    END IF;

    IF (v_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period term must match Course frequency';
    END IF;

    SET v_start_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.starts_month, 2, '0'), '-', LPAD(NEW.starts_day, 2, '0')), '%Y-%m-%d');
    SET v_end_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.ends_month, 2, '0'), '-', LPAD(NEW.ends_day, 2, '0')), '%Y-%m-%d');

    IF v_start_date IS NULL OR MONTH(v_start_date) <> NEW.starts_month OR DAY(v_start_date) <> NEW.starts_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period start month/day is invalid';
    END IF;

    IF v_end_date IS NULL OR MONTH(v_end_date) <> NEW.ends_month OR DAY(v_end_date) <> NEW.ends_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period end month/day is invalid';
    END IF;

END$$

DROP TRIGGER IF EXISTS bu_course_period_template_validate$$
CREATE TRIGGER bu_course_period_template_validate
BEFORE UPDATE ON course_period_template
FOR EACH ROW
BEGIN
    DECLARE v_duration INT;
    DECLARE v_frequency VARCHAR(20);
    DECLARE v_start_date DATE;
    DECLARE v_end_date DATE;

    SELECT CAST(duration AS UNSIGNED), frequency
    INTO v_duration, v_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period year must be inside Course duration';
    END IF;

    IF (v_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period term must match Course frequency';
    END IF;

    SET v_start_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.starts_month, 2, '0'), '-', LPAD(NEW.starts_day, 2, '0')), '%Y-%m-%d');
    SET v_end_date = STR_TO_DATE(CONCAT('2001-', LPAD(NEW.ends_month, 2, '0'), '-', LPAD(NEW.ends_day, 2, '0')), '%Y-%m-%d');

    IF v_start_date IS NULL OR MONTH(v_start_date) <> NEW.starts_month OR DAY(v_start_date) <> NEW.starts_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period start month/day is invalid';
    END IF;

    IF v_end_date IS NULL OR MONTH(v_end_date) <> NEW.ends_month OR DAY(v_end_date) <> NEW.ends_day THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course period end month/day is invalid';
    END IF;

END$$

DROP TRIGGER IF EXISTS bi_course_occurrence_validate$$
CREATE TRIGGER bi_course_occurrence_validate
BEFORE INSERT ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

    IF NEW.reference_year <> YEAR(NEW.starts_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence reference year must match its start year';
    END IF;

    IF NEW.label <> CONCAT(
        YEAR(NEW.starts_at),
        IF(YEAR(NEW.ends_at) > YEAR(NEW.starts_at), CONCAT('-', YEAR(NEW.ends_at)), '')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence label must match its concrete date range';
    END IF;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = NEW.id_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SELECT MIN(DATE_ADD(
               STR_TO_DATE(CONCAT(NEW.reference_year, '-', LPAD(starts_month, 2, '0'), '-', LPAD(starts_day, 2, '0')),
                           '%Y-%m-%d'),
               INTERVAL IF(
                   starts_month < v_anchor_start_month
                   OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                   1,
                   0
               ) YEAR
           )),
           MAX(DATE_ADD(
               STR_TO_DATE(CONCAT(
                   NEW.reference_year + IF(
                       starts_month < v_anchor_start_month
                       OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                       1,
                       0
                   ),
                   '-', LPAD(ends_month, 2, '0'), '-', LPAD(ends_day, 2, '0')
               ), '%Y-%m-%d'),
               INTERVAL IF(
                   ends_month < starts_month
                   OR (ends_month = starts_month AND ends_day < starts_day),
                   1,
                   0
               ) YEAR
           ))
    INTO v_expected_start, v_expected_end
    FROM course_period_template
    WHERE id_course = NEW.id_course;

    IF v_expected_start IS NULL
       OR NEW.starts_at <> v_expected_start
       OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence dates must match the configured Course calendar';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence existing_occurrence
        WHERE existing_occurrence.id_course = NEW.id_course
          AND NOT (
              NEW.ends_at < existing_occurrence.starts_at
              OR NEW.starts_at > existing_occurrence.ends_at
          )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence must start after the previous occurrence ends';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence_period cop
        WHERE cop.id_course_occurrence = NEW.id_course_occurrence
          AND (NEW.starts_at > cop.starts_at OR NEW.ends_at < cop.ends_at)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence dates must cover all occurrence periods';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_course_occurrence_validate$$
CREATE TRIGGER bu_course_occurrence_validate
BEFORE UPDATE ON course_occurrence
FOR EACH ROW
BEGIN
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;

    SET NEW.state = CASE
        WHEN NEW.state = 'cancelled' THEN 'cancelled'
        WHEN CURRENT_DATE < NEW.starts_at THEN 'scheduled'
        WHEN CURRENT_DATE > NEW.ends_at THEN 'completed'
        ELSE 'active'
    END;

    IF NEW.reference_year <> YEAR(NEW.starts_at) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence reference year must match its start year';
    END IF;

    IF NEW.label <> CONCAT(
        YEAR(NEW.starts_at),
        IF(YEAR(NEW.ends_at) > YEAR(NEW.starts_at), CONCAT('-', YEAR(NEW.ends_at)), '')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence label must match its concrete date range';
    END IF;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = NEW.id_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SELECT MIN(DATE_ADD(
               STR_TO_DATE(CONCAT(NEW.reference_year, '-', LPAD(starts_month, 2, '0'), '-', LPAD(starts_day, 2, '0')),
                           '%Y-%m-%d'),
               INTERVAL IF(
                   starts_month < v_anchor_start_month
                   OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                   1,
                   0
               ) YEAR
           )),
           MAX(DATE_ADD(
               STR_TO_DATE(CONCAT(
                   NEW.reference_year + IF(
                       starts_month < v_anchor_start_month
                       OR (starts_month = v_anchor_start_month AND starts_day < v_anchor_start_day),
                       1,
                       0
                   ),
                   '-', LPAD(ends_month, 2, '0'), '-', LPAD(ends_day, 2, '0')
               ), '%Y-%m-%d'),
               INTERVAL IF(
                   ends_month < starts_month
                   OR (ends_month = starts_month AND ends_day < starts_day),
                   1,
                   0
               ) YEAR
           ))
    INTO v_expected_start, v_expected_end
    FROM course_period_template
    WHERE id_course = NEW.id_course;

    IF v_expected_start IS NULL
       OR NEW.starts_at <> v_expected_start
       OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence dates must match the configured Course calendar';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence existing_occurrence
        WHERE existing_occurrence.id_course = NEW.id_course
          AND existing_occurrence.id_course_occurrence <> NEW.id_course_occurrence
          AND NOT (
              NEW.ends_at < existing_occurrence.starts_at
              OR NEW.starts_at > existing_occurrence.ends_at
          )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence must start after the previous occurrence ends';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM course_occurrence_period cop
        WHERE cop.id_course_occurrence = NEW.id_course_occurrence
          AND (NEW.starts_at > cop.starts_at OR NEW.ends_at < cop.ends_at)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence dates must cover all occurrence periods';
    END IF;

END$$

DROP TRIGGER IF EXISTS bi_course_occurrence_period_validate$$
CREATE TRIGGER bi_course_occurrence_period_validate
BEFORE INSERT ON course_occurrence_period
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_template_start_month TINYINT UNSIGNED;
    DECLARE v_template_start_day TINYINT UNSIGNED;
    DECLARE v_template_end_month TINYINT UNSIGNED;
    DECLARE v_template_end_day TINYINT UNSIGNED;
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;
    DECLARE v_overlap_count INT DEFAULT 0;

    SELECT id_course, starts_at, ends_at, state
    INTO v_occurrence_course, v_occurrence_start, v_occurrence_end, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF NOT EXISTS (
        SELECT 1
        FROM course_period_template
        WHERE id_course = v_occurrence_course
          AND curricular_year = NEW.curricular_year
          AND term = NEW.term
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must match a configured Course period';
    END IF;

    SELECT starts_month, starts_day, ends_month, ends_day
    INTO v_template_start_month, v_template_start_day, v_template_end_month, v_template_end_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
      AND curricular_year = NEW.curricular_year
      AND term = NEW.term;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SET v_expected_start = STR_TO_DATE(
        CONCAT(YEAR(v_occurrence_start), '-', LPAD(v_template_start_month, 2, '0'), '-', LPAD(v_template_start_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_template_start_month < v_anchor_start_month
       OR (v_template_start_month = v_anchor_start_month AND v_template_start_day < v_anchor_start_day) THEN
        SET v_expected_start = DATE_ADD(v_expected_start, INTERVAL 1 YEAR);
    END IF;
    SET v_expected_end = STR_TO_DATE(
        CONCAT(YEAR(v_expected_start), '-', LPAD(v_template_end_month, 2, '0'), '-', LPAD(v_template_end_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_expected_end < v_expected_start THEN
        SET v_expected_end = DATE_ADD(v_expected_end, INTERVAL 1 YEAR);
    END IF;

    IF NEW.starts_at <> v_expected_start OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence period dates must match the configured Course period';
    END IF;

    IF NEW.starts_at < v_occurrence_start OR NEW.ends_at > v_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must stay inside the Course occurrence date range';
    END IF;

    IF NEW.state IN ('active', 'completed') AND v_occurrence_state NOT IN ('active', 'completed') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active or completed Course occurrence period requires an active or completed Course occurrence';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_course_occurrence_period_validate$$
CREATE TRIGGER bu_course_occurrence_period_validate
BEFORE UPDATE ON course_occurrence_period
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_template_start_month TINYINT UNSIGNED;
    DECLARE v_template_start_day TINYINT UNSIGNED;
    DECLARE v_template_end_month TINYINT UNSIGNED;
    DECLARE v_template_end_day TINYINT UNSIGNED;
    DECLARE v_anchor_start_month TINYINT UNSIGNED;
    DECLARE v_anchor_start_day TINYINT UNSIGNED;
    DECLARE v_expected_start DATE;
    DECLARE v_expected_end DATE;
    DECLARE v_overlap_count INT DEFAULT 0;

    SELECT id_course, starts_at, ends_at, state
    INTO v_occurrence_course, v_occurrence_start, v_occurrence_end, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF NOT EXISTS (
        SELECT 1
        FROM course_period_template
        WHERE id_course = v_occurrence_course
          AND curricular_year = NEW.curricular_year
          AND term = NEW.term
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must match a configured Course period';
    END IF;

    SELECT starts_month, starts_day, ends_month, ends_day
    INTO v_template_start_month, v_template_start_day, v_template_end_month, v_template_end_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
      AND curricular_year = NEW.curricular_year
      AND term = NEW.term;

    SELECT starts_month, starts_day
    INTO v_anchor_start_month, v_anchor_start_day
    FROM course_period_template
    WHERE id_course = v_occurrence_course
    ORDER BY curricular_year,
             FIELD(term,
                   'annual',
                   'semester_1', 'semester_2',
                   'quadrimester_1', 'quadrimester_2', 'quadrimester_3',
                   'trimester_1', 'trimester_2', 'trimester_3', 'trimester_4',
                   'bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6',
                   'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
                   'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12')
    LIMIT 1;

    SET v_expected_start = STR_TO_DATE(
        CONCAT(YEAR(v_occurrence_start), '-', LPAD(v_template_start_month, 2, '0'), '-', LPAD(v_template_start_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_template_start_month < v_anchor_start_month
       OR (v_template_start_month = v_anchor_start_month AND v_template_start_day < v_anchor_start_day) THEN
        SET v_expected_start = DATE_ADD(v_expected_start, INTERVAL 1 YEAR);
    END IF;
    SET v_expected_end = STR_TO_DATE(
        CONCAT(YEAR(v_expected_start), '-', LPAD(v_template_end_month, 2, '0'), '-', LPAD(v_template_end_day, 2, '0')),
        '%Y-%m-%d'
    );
    IF v_expected_end < v_expected_start THEN
        SET v_expected_end = DATE_ADD(v_expected_end, INTERVAL 1 YEAR);
    END IF;

    IF NEW.starts_at <> v_expected_start OR NEW.ends_at <> v_expected_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Course occurrence period dates must match the configured Course period';
    END IF;

    IF NEW.starts_at < v_occurrence_start OR NEW.ends_at > v_occurrence_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course occurrence period must stay inside the Course occurrence date range';
    END IF;

    IF NEW.state IN ('active', 'completed') AND v_occurrence_state NOT IN ('active', 'completed') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active or completed Course occurrence period requires an active or completed Course occurrence';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_subject_validate$$
CREATE TRIGGER bi_subject_validate
BEFORE INSERT ON subject
FOR EACH ROW
BEGIN
    DECLARE v_organization_state VARCHAR(20);
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    SELECT state
    INTO v_organization_state
    FROM organization
    WHERE id_organization = NEW.id_organization;

    IF NEW.state = 'active' AND v_organization_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject cannot be active in inactive Organization';
    END IF;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_subject_validate$$
CREATE TRIGGER bu_subject_validate
BEFORE UPDATE ON subject
FOR EACH ROW
BEGIN
    DECLARE v_organization_state VARCHAR(20);
    DECLARE v_organic_unit_org BIGINT UNSIGNED;

    SELECT state
    INTO v_organization_state
    FROM organization
    WHERE id_organization = NEW.id_organization;

    IF NEW.state = 'active' AND v_organization_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject cannot be active in inactive Organization';
    END IF;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Subject Organic_Unit must belong to the same Organization';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_coordinate_subject_validate$$
CREATE TRIGGER bi_coordinate_subject_validate
BEFORE INSERT ON coordinate_subject
FOR EACH ROW
BEGIN
    DECLARE v_coordinator_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);

    IF NEW.state = 'active' THEN
        SELECT state
        INTO v_coordinator_state
        FROM user_account
        WHERE id_user = NEW.id_coordinator_user;

        SELECT state
        INTO v_subject_state
        FROM subject
        WHERE id_subject = NEW.id_subject;

        IF v_coordinator_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Coordinator';
        END IF;
        IF v_subject_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Subject';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_coordinate_subject_validate$$
CREATE TRIGGER bu_coordinate_subject_validate
BEFORE UPDATE ON coordinate_subject
FOR EACH ROW
BEGIN
    DECLARE v_coordinator_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);

    IF NEW.state = 'active' AND OLD.state <> 'active' THEN
        SELECT state
        INTO v_coordinator_state
        FROM user_account
        WHERE id_user = NEW.id_coordinator_user;

        SELECT state
        INTO v_subject_state
        FROM subject
        WHERE id_subject = NEW.id_subject;

        IF v_coordinator_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Coordinator';
        END IF;
        IF v_subject_state <> 'active' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
                'Active coordinator assignment requires an active Subject';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_integrate_subject_validate$$
CREATE TRIGGER bi_integrate_subject_validate
BEFORE INSERT ON integrate_subject
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT state
    INTO v_subject_state
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires an active Course';
    END IF;

    IF NEW.state = 'active' AND v_subject_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Active Course_Subject association requires an active Subject';
    END IF;

    IF NEW.curricular_year IS NULL OR NEW.term IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires course year and period';
    END IF;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_course_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject course year must stay inside Course duration';
    END IF;

    IF (v_course_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_course_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_course_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_course_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_course_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_course_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject term must match Course frequency';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_integrate_subject_validate$$
CREATE TRIGGER bu_integrate_subject_validate
BEFORE UPDATE ON integrate_subject
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_course_duration INT;
    DECLARE v_course_frequency VARCHAR(20);

    SELECT state, CAST(duration AS UNSIGNED), frequency
    INTO v_course_state, v_course_duration, v_course_frequency
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT state
    INTO v_subject_state
    FROM subject
    WHERE id_subject = NEW.id_subject;

    IF v_course_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires an active Course';
    END IF;

    IF NEW.state = 'active' AND v_subject_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Active Course_Subject association requires an active Subject';
    END IF;

    IF NEW.curricular_year IS NULL OR NEW.term IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject association requires course year and period';
    END IF;

    IF NEW.curricular_year < 1 OR NEW.curricular_year > v_course_duration THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject course year must stay inside Course duration';
    END IF;

    IF (v_course_frequency = 'annual' AND NEW.term <> 'annual')
       OR (v_course_frequency = 'semester' AND NEW.term NOT IN ('semester_1', 'semester_2'))
       OR (v_course_frequency = 'quadrimester' AND NEW.term NOT IN ('quadrimester_1', 'quadrimester_2', 'quadrimester_3'))
       OR (v_course_frequency = 'trimester' AND NEW.term NOT IN ('trimester_1', 'trimester_2', 'trimester_3', 'trimester_4'))
       OR (v_course_frequency = 'bimonthly' AND NEW.term NOT IN ('bimester_1', 'bimester_2', 'bimester_3', 'bimester_4', 'bimester_5', 'bimester_6'))
       OR (v_course_frequency = 'monthly' AND NEW.term NOT IN (
            'month_1', 'month_2', 'month_3', 'month_4', 'month_5', 'month_6',
            'month_7', 'month_8', 'month_9', 'month_10', 'month_11', 'month_12'
       )) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course_Subject term must match Course frequency';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_enroll_course_validate$$
CREATE TRIGGER bi_enroll_course_validate
BEFORE INSERT ON enroll_course
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_student_count INT DEFAULT 0;

    SELECT state
    INTO v_course_state
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_course, state, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_state, v_occurrence_start, v_occurrence_end
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment occurrence must belong to the selected Course';
    END IF;

    IF NEW.start_date IS NOT NULL
       AND NEW.start_date <> '1000-01-01'
       AND (NEW.start_date < v_occurrence_start OR NEW.start_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment start date must stay inside the Course occurrence';
    END IF;

    IF NEW.end_date IS NOT NULL
       AND NEW.end_date <> '1000-01-01'
       AND (NEW.end_date < v_occurrence_start OR NEW.end_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment end date must stay inside the Course occurrence';
    END IF;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_occurrence_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_occurrence_end
        ELSE NEW.end_date
    END;

    IF NEW.state = 'active' AND (
        v_course_state <> 'active'
        OR v_occurrence_state NOT IN ('scheduled', 'active')
        OR CURRENT_DATE > v_occurrence_end
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires scheduled or active Course occurrence';
    END IF;

    IF NEW.state = 'active' AND v_student_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Student';
    END IF;

END$$

DROP TRIGGER IF EXISTS bu_enroll_course_validate$$
CREATE TRIGGER bu_enroll_course_validate
BEFORE UPDATE ON enroll_course
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_occurrence_start DATE;
    DECLARE v_occurrence_end DATE;
    DECLARE v_student_count INT DEFAULT 0;

    SELECT state
    INTO v_course_state
    FROM course
    WHERE id_course = NEW.id_course;

    SELECT id_course, state, starts_at, ends_at
    INTO v_occurrence_course, v_occurrence_state, v_occurrence_start, v_occurrence_end
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment occurrence must belong to the selected Course';
    END IF;

    IF NEW.start_date IS NOT NULL
       AND NEW.start_date <> '1000-01-01'
       AND (NEW.start_date < v_occurrence_start OR NEW.start_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment start date must stay inside the Course occurrence';
    END IF;

    IF NEW.end_date IS NOT NULL
       AND NEW.end_date <> '1000-01-01'
       AND (NEW.end_date < v_occurrence_start OR NEW.end_date > v_occurrence_end) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Course enrollment end date must stay inside the Course occurrence';
    END IF;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_occurrence_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_occurrence_end
        ELSE NEW.end_date
    END;

    IF NEW.state = 'active' AND (
        v_course_state <> 'active'
        OR v_occurrence_state NOT IN ('scheduled', 'active')
        OR CURRENT_DATE > v_occurrence_end
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires scheduled or active Course occurrence';
    END IF;

    IF NEW.state = 'active' AND v_student_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Course enrollment requires active Student';
    END IF;

END$$

DROP TRIGGER IF EXISTS bi_class_group_validate$$
CREATE TRIGGER bi_class_group_validate
BEFORE INSERT ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_state VARCHAR(20);
    DECLARE v_association_year INT;
    DECLARE v_association_term VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_period_occurrence BIGINT UNSIGNED;
    DECLARE v_period_year INT;
    DECLARE v_period_term VARCHAR(20);
    DECLARE v_period_start DATE;
    DECLARE v_period_end DATE;
    DECLARE v_period_state VARCHAR(20);

    SELECT c.state, s.state, isub.state, isub.curricular_year, isub.term
    INTO v_course_state, v_subject_state, v_association_state, v_association_year, v_association_term
    FROM integrate_subject isub
    JOIN course c ON c.id_course = isub.id_course
    JOIN subject s ON s.id_subject = isub.id_subject
    WHERE isub.id_course = NEW.id_course
      AND isub.id_subject = NEW.id_subject;

    SELECT id_course, state
    INTO v_occurrence_course, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT id_course_occurrence, curricular_year, term, starts_at, ends_at, state
    INTO v_period_occurrence, v_period_year, v_period_term, v_period_start, v_period_end, v_period_state
    FROM course_occurrence_period
    WHERE id_course_occurrence_period = NEW.id_course_occurrence_period;

    IF v_association_year IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course must integrate the selected Subject';
    END IF;
    IF v_association_state <> 'active' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'New Class_Group requires an active Course_Subject association';
    END IF;

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course occurrence must belong to the selected Course';
    END IF;

    IF v_period_occurrence <> NEW.id_course_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must belong to the selected Course occurrence';
    END IF;

    IF v_association_year <> v_period_year OR v_association_term <> v_period_term THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must match the Subject curricular position in the Course';
    END IF;

    IF NEW.starts_at <> v_period_start OR NEW.ends_at <> v_period_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group dates must match the selected Course occurrence period';
    END IF;

    IF NEW.state = 'scheduled'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state NOT IN ('scheduled', 'active')
            OR v_period_state NOT IN ('scheduled', 'active')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduled Class_Group requires a scheduled or active occurrence context';
    END IF;

    IF NEW.state = 'active'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state <> 'active'
            OR v_period_state <> 'active') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group requires an active occurrence context';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_class_group_validate$$
CREATE TRIGGER bu_class_group_validate
BEFORE UPDATE ON class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_state VARCHAR(20);
    DECLARE v_association_year INT;
    DECLARE v_association_term VARCHAR(20);
    DECLARE v_occurrence_course BIGINT UNSIGNED;
    DECLARE v_occurrence_state VARCHAR(20);
    DECLARE v_period_occurrence BIGINT UNSIGNED;
    DECLARE v_period_year INT;
    DECLARE v_period_term VARCHAR(20);
    DECLARE v_period_start DATE;
    DECLARE v_period_end DATE;
    DECLARE v_period_state VARCHAR(20);
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT c.state, s.state, isub.state, isub.curricular_year, isub.term
    INTO v_course_state, v_subject_state, v_association_state, v_association_year, v_association_term
    FROM integrate_subject isub
    JOIN course c ON c.id_course = isub.id_course
    JOIN subject s ON s.id_subject = isub.id_subject
    WHERE isub.id_course = NEW.id_course
      AND isub.id_subject = NEW.id_subject;

    SELECT id_course, state
    INTO v_occurrence_course, v_occurrence_state
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    SELECT id_course_occurrence, curricular_year, term, starts_at, ends_at, state
    INTO v_period_occurrence, v_period_year, v_period_term, v_period_start, v_period_end, v_period_state
    FROM course_occurrence_period
    WHERE id_course_occurrence_period = NEW.id_course_occurrence_period;

    IF v_association_year IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course must integrate the selected Subject';
    END IF;
    IF v_association_state <> 'active'
       AND (NEW.id_course <> OLD.id_course
            OR NEW.id_subject <> OLD.id_subject
            OR NEW.id_course_occurrence <> OLD.id_course_occurrence
            OR NEW.id_course_occurrence_period <> OLD.id_course_occurrence_period) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Historical Course_Subject associations cannot receive or move Class_Groups';
    END IF;

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group Course occurrence must belong to the selected Course';
    END IF;

    IF v_period_occurrence <> NEW.id_course_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must belong to the selected Course occurrence';
    END IF;

    IF v_association_year <> v_period_year OR v_association_term <> v_period_term THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group period must match the Subject curricular position in the Course';
    END IF;

    IF NEW.starts_at <> v_period_start OR NEW.ends_at <> v_period_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group dates must match the selected Course occurrence period';
    END IF;

    IF NEW.state = 'scheduled'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state NOT IN ('scheduled', 'active')
            OR v_period_state NOT IN ('scheduled', 'active')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Scheduled Class_Group requires a scheduled or active occurrence context';
    END IF;

    IF NEW.state = 'active'
       AND (v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_occurrence_state <> 'active'
            OR v_period_state <> 'active') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group requires an active occurrence context';
    END IF;

    IF NEW.max_students IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_active_enrollments
        FROM enroll_class_group
        WHERE id_class_group = NEW.id_class_group
          AND state = 'active';

        IF v_active_enrollments > NEW.max_students THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group max_students cannot be below active enrollments';
        END IF;
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_teach_class_group_validate$$
CREATE TRIGGER bi_teach_class_group_validate
BEFORE INSERT ON teach_class_group
FOR EACH ROW
BEGIN
    DECLARE v_teacher_count INT DEFAULT 0;
    DECLARE v_class_state VARCHAR(20);

    SELECT COUNT(*)
    INTO v_teacher_count
    FROM teacher_profile tp
    JOIN user_account u ON u.id_user = tp.id_user
    WHERE tp.id_user = NEW.id_teacher_user
      AND u.state = 'active';

    SELECT state
    INTO v_class_state
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF NEW.state = 'active' AND (v_teacher_count = 0 OR v_class_state <> 'active') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group teaching assignment requires active Teacher and Class_Group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_teach_class_group_validate$$
CREATE TRIGGER bu_teach_class_group_validate
BEFORE UPDATE ON teach_class_group
FOR EACH ROW
BEGIN
    DECLARE v_teacher_count INT DEFAULT 0;
    DECLARE v_class_state VARCHAR(20);

    SELECT COUNT(*)
    INTO v_teacher_count
    FROM teacher_profile tp
    JOIN user_account u ON u.id_user = tp.id_user
    WHERE tp.id_user = NEW.id_teacher_user
      AND u.state = 'active';

    SELECT state
    INTO v_class_state
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF NEW.state = 'active' AND (v_teacher_count = 0 OR v_class_state <> 'active') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group teaching assignment requires active Teacher and Class_Group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_enroll_class_group_validate$$
CREATE TRIGGER bi_enroll_class_group_validate
BEFORE INSERT ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_subject_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_class_start DATE;
    DECLARE v_class_end DATE;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_subject, cg.id_course_occurrence, cg.starts_at, cg.ends_at,
           cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_subject_id, v_course_occurrence_id, v_class_start, v_class_end,
         v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    -- The class-group occurrence owns an enrollment's period.  Clients may
    -- request an enrollment, but cannot choose a different start/end range.
    SET NEW.start_date = v_class_start;
    SET NEW.end_date = CASE
        WHEN NEW.state = 'withdrawn' THEN LEAST(
            v_class_end,
            GREATEST(v_class_start, COALESCE(NULLIF(NEW.end_date, '1000-01-01'), CURRENT_DATE))
        )
        ELSE v_class_end
    END;

    IF NEW.start_date < v_class_start OR NEW.start_date > v_class_end
       OR NEW.end_date < v_class_start OR NEW.end_date > v_class_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment dates must stay inside the Class_Group occurrence period';
    END IF;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    SELECT COUNT(*)
    INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND (ec.start_date IS NULL OR NEW.start_date IS NULL OR ec.start_date <= NEW.start_date)
      AND (NEW.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= NEW.end_date);

    SELECT COUNT(*)
    INTO v_overlap_count
    FROM enroll_class_group ecg
    JOIN class_group existing_cg ON existing_cg.id_class_group = ecg.id_class_group
    WHERE ecg.id_student_user = NEW.id_student_user
      AND existing_cg.id_course = v_course_id
      AND existing_cg.id_subject = v_subject_id
      AND existing_cg.id_course_occurrence = v_course_occurrence_id
      AND ecg.state = 'active'
      AND (ecg.start_date IS NULL OR NEW.end_date IS NULL OR ecg.start_date <= NEW.end_date)
      AND (ecg.end_date IS NULL OR NEW.start_date IS NULL OR ecg.end_date >= NEW.start_date);

    SELECT COUNT(*)
    INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group
      AND state = 'active';

    IF NEW.state = 'active'
       AND (v_student_count = 0
            OR v_class_state <> 'active'
            OR v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_association_course IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group enrollment requires active Student, Class_Group, Course, Subject and course-subject association';
    END IF;

    IF NEW.state = 'active' AND v_course_enrollment_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment requires active Course occurrence enrollment for the full period';
    END IF;

    IF NEW.state = 'active' AND v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Student already has an overlapping active enrollment in this Course/Subject class group context';
    END IF;

    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_enroll_class_group_validate$$
CREATE TRIGGER bu_enroll_class_group_validate
BEFORE UPDATE ON enroll_class_group
FOR EACH ROW
BEGIN
    DECLARE v_course_id BIGINT UNSIGNED;
    DECLARE v_subject_id BIGINT UNSIGNED;
    DECLARE v_course_occurrence_id BIGINT UNSIGNED;
    DECLARE v_class_start DATE;
    DECLARE v_class_end DATE;
    DECLARE v_max_students INT;
    DECLARE v_class_state VARCHAR(20);
    DECLARE v_course_state VARCHAR(20);
    DECLARE v_subject_state VARCHAR(20);
    DECLARE v_association_course BIGINT UNSIGNED;
    DECLARE v_student_count INT DEFAULT 0;
    DECLARE v_course_enrollment_count INT DEFAULT 0;
    DECLARE v_overlap_count INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;

    SELECT cg.id_course, cg.id_subject, cg.id_course_occurrence, cg.starts_at, cg.ends_at,
           cg.max_students, cg.state, c.state, s.state, isub.id_course
    INTO v_course_id, v_subject_id, v_course_occurrence_id, v_class_start, v_class_end,
         v_max_students, v_class_state, v_course_state, v_subject_state, v_association_course
    FROM class_group cg
    JOIN course c ON c.id_course = cg.id_course
    JOIN subject s ON s.id_subject = cg.id_subject
    LEFT JOIN integrate_subject isub ON isub.id_course = cg.id_course AND isub.id_subject = cg.id_subject
    WHERE cg.id_class_group = NEW.id_class_group;

    -- The class-group occurrence owns an enrollment's period.  State changes
    -- never reopen manual editing of the enrollment range.
    SET NEW.start_date = v_class_start;
    SET NEW.end_date = CASE
        WHEN NEW.state = 'withdrawn' THEN LEAST(
            v_class_end,
            GREATEST(v_class_start, COALESCE(NULLIF(NEW.end_date, '1000-01-01'), CURRENT_DATE))
        )
        ELSE v_class_end
    END;

    IF NEW.start_date < v_class_start OR NEW.start_date > v_class_end
       OR NEW.end_date < v_class_start OR NEW.end_date > v_class_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment dates must stay inside the Class_Group occurrence period';
    END IF;

    SELECT COUNT(*)
    INTO v_student_count
    FROM student_profile sp
    JOIN user_account u ON u.id_user = sp.id_user
    WHERE sp.id_user = NEW.id_student_user
      AND u.state = 'active';

    SELECT COUNT(*)
    INTO v_course_enrollment_count
    FROM enroll_course ec
    WHERE ec.id_student_user = NEW.id_student_user
      AND ec.id_course = v_course_id
      AND ec.id_course_occurrence = v_course_occurrence_id
      AND ec.state = 'active'
      AND (ec.start_date IS NULL OR NEW.start_date IS NULL OR ec.start_date <= NEW.start_date)
      AND (NEW.end_date IS NULL OR ec.end_date IS NULL OR ec.end_date >= NEW.end_date);

    SELECT COUNT(*)
    INTO v_overlap_count
    FROM enroll_class_group ecg
    JOIN class_group existing_cg ON existing_cg.id_class_group = ecg.id_class_group
    WHERE ecg.id_student_user = NEW.id_student_user
      AND existing_cg.id_course = v_course_id
      AND existing_cg.id_subject = v_subject_id
      AND existing_cg.id_course_occurrence = v_course_occurrence_id
      AND ecg.state = 'active'
      AND NOT (ecg.id_student_user = OLD.id_student_user AND ecg.id_class_group = OLD.id_class_group)
      AND (ecg.start_date IS NULL OR NEW.end_date IS NULL OR ecg.start_date <= NEW.end_date)
      AND (ecg.end_date IS NULL OR NEW.start_date IS NULL OR ecg.end_date >= NEW.start_date);

    SELECT COUNT(*)
    INTO v_active_enrollments
    FROM enroll_class_group
    WHERE id_class_group = NEW.id_class_group
      AND state = 'active'
      AND NOT (id_student_user = OLD.id_student_user AND id_class_group = OLD.id_class_group);

    IF NEW.state = 'active'
       AND (v_student_count = 0
            OR v_class_state <> 'active'
            OR v_course_state <> 'active'
            OR v_subject_state <> 'active'
            OR v_association_course IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Class_Group enrollment requires active Student, Class_Group, Course, Subject and course-subject association';
    END IF;

    IF NEW.state = 'active' AND v_course_enrollment_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group enrollment requires active Course occurrence enrollment for the full period';
    END IF;

    IF NEW.state = 'active' AND v_overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Student already has an overlapping active enrollment in this Course/Subject class group context';
    END IF;

    IF NEW.state = 'active' AND v_max_students IS NOT NULL AND v_active_enrollments >= v_max_students THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Class_Group maximum capacity exceeded';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_enroll_assessment_validate$$
CREATE TRIGGER bi_enroll_assessment_validate
BEFORE INSERT ON enroll_assessment
FOR EACH ROW
BEGIN
    DECLARE v_available_start DATE;
    DECLARE v_available_end DATE;

    SELECT DATE(available_from), DATE(available_until)
    INTO v_available_start, v_available_end
    FROM assessment
    WHERE id_assessment = NEW.id_assessment;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_available_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_available_end
        ELSE NEW.end_date
    END;

    IF NEW.start_date < v_available_start OR NEW.start_date > v_available_end
       OR NEW.end_date < v_available_start OR NEW.end_date > v_available_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment enrollment dates must stay inside the assessment availability period';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_enroll_assessment_validate$$
CREATE TRIGGER bu_enroll_assessment_validate
BEFORE UPDATE ON enroll_assessment
FOR EACH ROW
BEGIN
    DECLARE v_available_start DATE;
    DECLARE v_available_end DATE;

    SELECT DATE(available_from), DATE(available_until)
    INTO v_available_start, v_available_end
    FROM assessment
    WHERE id_assessment = NEW.id_assessment;

    SET NEW.start_date = CASE
        WHEN NEW.start_date IS NULL OR NEW.start_date = '1000-01-01' THEN v_available_start
        ELSE NEW.start_date
    END;
    SET NEW.end_date = CASE
        WHEN NEW.end_date IS NULL OR NEW.end_date = '1000-01-01' THEN v_available_end
        ELSE NEW.end_date
    END;

    IF NEW.start_date < v_available_start OR NEW.start_date > v_available_end
       OR NEW.end_date < v_available_start OR NEW.end_date > v_available_end THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment enrollment dates must stay inside the assessment availability period';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_content_block_validate$$
CREATE TRIGGER bi_content_block_validate
BEFORE INSERT ON content_block
FOR EACH ROW
BEGIN
    DECLARE v_duplicate_order_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_duplicate_order_count
    FROM content_block
    WHERE id_class_group = NEW.id_class_group
      AND order_no = NEW.order_no;

    IF v_duplicate_order_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Content_Block order must be unique in the Class_Group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_content_block_validate$$
CREATE TRIGGER bu_content_block_validate
BEFORE UPDATE ON content_block
FOR EACH ROW
BEGIN
    DECLARE v_duplicate_order_count INT DEFAULT 0;

    IF NEW.id_class_group <> OLD.id_class_group THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Content_Block Class_Group cannot be changed after creation';
    END IF;

    SELECT COUNT(*)
    INTO v_duplicate_order_count
    FROM content_block
    WHERE id_class_group = NEW.id_class_group
      AND order_no = NEW.order_no
      AND id_content_block <> NEW.id_content_block;

    IF v_duplicate_order_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Content_Block order must be unique in the Class_Group';
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
    DECLARE v_oversized_reserved_lessons INT DEFAULT 0;

    IF NEW.id_organic_unit IS NOT NULL THEN
        SELECT id_organization
        INTO v_organic_unit_org
        FROM organic_unit
        WHERE id_organic_unit = NEW.id_organic_unit;

        IF v_organic_unit_org IS NULL OR v_organic_unit_org <> NEW.id_organization THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room Organic_Unit must belong to the same Organization';
        END IF;
    END IF;

    SELECT COUNT(*)
    INTO v_oversized_reserved_lessons
    FROM lesson l
    WHERE l.cod_physical_room = NEW.cod_physical_room
      AND l.state IN ('scheduled', 'active')
      AND l.type IN ('onsite', 'hybrid')
      AND (
          SELECT COUNT(*)
          FROM enroll_class_group ecg
          WHERE ecg.id_class_group = l.id_class_group
            AND ecg.state = 'active'
            AND (ecg.start_date IS NULL OR ecg.start_date <= CURRENT_DATE)
            AND (ecg.end_date IS NULL OR ecg.end_date >= CURRENT_DATE)
      ) > NEW.capacity;

    IF v_oversized_reserved_lessons > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room capacity is below active Class_Group enrollments';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_content_item_validate$$
CREATE TRIGGER bi_content_item_validate
BEFORE INSERT ON content_item
FOR EACH ROW
BEGIN
    IF NEW.format IN ('text', 'image', 'video', 'audio', 'pdf', 'archive', 'url', 'scorm', 'xapi', 'presentation', 'embed')
       AND COALESCE(TRIM(NEW.source), '') = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Content_Item source is required for the selected format';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_content_item_validate$$
CREATE TRIGGER bu_content_item_validate
BEFORE UPDATE ON content_item
FOR EACH ROW
BEGIN
    IF NEW.format IN ('text', 'image', 'video', 'audio', 'pdf', 'archive', 'url', 'scorm', 'xapi', 'presentation', 'embed')
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
    DECLARE v_room_capacity INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;
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
        SELECT state, id_organization, capacity
        INTO v_room_state, v_room_org, v_room_capacity
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

        SELECT COUNT(*)
        INTO v_active_enrollments
        FROM enroll_class_group
        WHERE id_class_group = NEW.id_class_group
          AND state = 'active'
          AND (start_date IS NULL OR start_date <= CURRENT_DATE)
          AND (end_date IS NULL OR end_date >= CURRENT_DATE);

        IF v_room_capacity < v_active_enrollments THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room capacity is below active Class_Group enrollments';
        END IF;

        IF NEW.state IN ('scheduled', 'active') AND NEW.type IN ('onsite', 'hybrid') THEN
            SELECT COUNT(*)
            INTO v_overlap_count
            FROM lesson l
            WHERE l.cod_physical_room = NEW.cod_physical_room
              AND l.state IN ('scheduled', 'active')
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
    DECLARE v_room_capacity INT DEFAULT 0;
    DECLARE v_active_enrollments INT DEFAULT 0;
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
        SELECT state, id_organization, capacity
        INTO v_room_state, v_room_org, v_room_capacity
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

        SELECT COUNT(*)
        INTO v_active_enrollments
        FROM enroll_class_group
        WHERE id_class_group = NEW.id_class_group
          AND state = 'active'
          AND (start_date IS NULL OR start_date <= CURRENT_DATE)
          AND (end_date IS NULL OR end_date >= CURRENT_DATE);

        IF v_room_capacity < v_active_enrollments THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Physical_Room capacity is below active Class_Group enrollments';
        END IF;

        IF NEW.state IN ('scheduled', 'active') AND NEW.type IN ('onsite', 'hybrid') THEN
            SELECT COUNT(*)
            INTO v_overlap_count
            FROM lesson l
            WHERE l.cod_physical_room = NEW.cod_physical_room
              AND l.state IN ('scheduled', 'active')
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

    IF NEW.type IN ('form', 'test') AND NEW.id_content_block IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Form and Test Assessments require a Content_Block';
    END IF;

    IF NEW.type = 'exam' AND NEW.id_subject IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exam Assessment requires a Subject';
    END IF;

    IF NEW.mode = 'onsite'
       AND (NEW.cod_physical_room IS NULL OR NEW.available_from IS NULL OR NEW.available_until IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Onsite Assessment requires a Physical_Room and availability window';
    END IF;

    IF NEW.mode = 'online' AND NEW.cod_physical_room IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Online Assessment cannot reserve a Physical_Room';
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

    IF NEW.type IN ('form', 'test') AND NEW.id_content_block IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Form and Test Assessments require a Content_Block';
    END IF;

    IF NEW.type = 'exam' AND NEW.id_subject IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Exam Assessment requires a Subject';
    END IF;

    IF NEW.mode = 'onsite'
       AND (NEW.cod_physical_room IS NULL OR NEW.available_from IS NULL OR NEW.available_until IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Onsite Assessment requires a Physical_Room and availability window';
    END IF;

    IF NEW.mode = 'online' AND NEW.cod_physical_room IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Online Assessment cannot reserve a Physical_Room';
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

DROP TRIGGER IF EXISTS bi_assessment_class_group_validate$$
CREATE TRIGGER bi_assessment_class_group_validate
BEFORE INSERT ON assessment_class_group
FOR EACH ROW
BEGIN
    DECLARE v_assessment_type VARCHAR(30);
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_assessment_block BIGINT UNSIGNED;
    DECLARE v_group_subject BIGINT UNSIGNED;

    SELECT type, id_subject, id_content_block
    INTO v_assessment_type, v_assessment_subject, v_assessment_block
    FROM assessment
    WHERE id_assessment = NEW.id_assessment;

    SELECT id_subject
    INTO v_group_subject
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF v_assessment_type <> 'exam' OR v_assessment_block IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment_Class_Group is only allowed for subject-level Exam Assessments';
    END IF;

    IF v_assessment_subject IS NULL OR v_group_subject <> v_assessment_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment_Class_Group Class_Group must belong to the Assessment Subject';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_assessment_class_group_validate$$
CREATE TRIGGER bu_assessment_class_group_validate
BEFORE UPDATE ON assessment_class_group
FOR EACH ROW
BEGIN
    DECLARE v_assessment_type VARCHAR(30);
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_assessment_block BIGINT UNSIGNED;
    DECLARE v_group_subject BIGINT UNSIGNED;

    SELECT type, id_subject, id_content_block
    INTO v_assessment_type, v_assessment_subject, v_assessment_block
    FROM assessment
    WHERE id_assessment = NEW.id_assessment;

    SELECT id_subject
    INTO v_group_subject
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF v_assessment_type <> 'exam' OR v_assessment_block IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment_Class_Group is only allowed for subject-level Exam Assessments';
    END IF;

    IF v_assessment_subject IS NULL OR v_group_subject <> v_assessment_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assessment_Class_Group Class_Group must belong to the Assessment Subject';
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
    DECLARE v_assessment_state VARCHAR(20);
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_assessment_enrollment_exists INT DEFAULT 0;

    SELECT a.attempts_limit, a.id_subject, cb.id_class_group, a.state
    INTO v_attempts_limit, v_assessment_subject, v_class_group, v_assessment_state
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_attempts_limit IS NOT NULL AND NEW.attempt_number > v_attempts_limit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt number exceeds Assessment.attempts_limit';
    END IF;

    IF NEW.state IN ('submitted', 'corrected') AND NEW.submitted_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted or corrected Attempt requires submitted_at';
    END IF;

    IF NEW.state <> 'corrected' AND NEW.score IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only corrected Attempt can store score';
    END IF;

    IF v_class_group IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM enroll_class_group
        WHERE id_student_user = NEW.id_student_user
          AND id_class_group = v_class_group
          AND (state = 'active' OR (v_assessment_state = 'completed' AND state = 'completed'));
    ELSEIF v_assessment_subject IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM assessment_class_group acg
        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
        JOIN enroll_class_group ecg
          ON ecg.id_class_group = cg.id_class_group
         AND ecg.id_student_user = NEW.id_student_user
         AND (ecg.state = 'active' OR (v_assessment_state = 'completed' AND ecg.state = 'completed'))
        WHERE acg.id_assessment = NEW.id_assessment
          AND cg.id_subject = v_assessment_subject;
    END IF;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active enrollment in the Assessment context';
    END IF;

    SELECT COUNT(*)
    INTO v_assessment_enrollment_exists
    FROM enroll_assessment
    WHERE id_student_user = NEW.id_student_user
      AND id_assessment = NEW.id_assessment
      AND state = 'active';

    IF v_assessment_enrollment_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active Assessment enrollment';
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
    DECLARE v_assessment_state VARCHAR(20);
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_assessment_enrollment_exists INT DEFAULT 0;

    SELECT a.attempts_limit, a.id_subject, cb.id_class_group, a.state
    INTO v_attempts_limit, v_assessment_subject, v_class_group, v_assessment_state
    FROM assessment a
    LEFT JOIN content_block cb ON cb.id_content_block = a.id_content_block
    WHERE a.id_assessment = NEW.id_assessment;

    IF v_attempts_limit IS NOT NULL AND NEW.attempt_number > v_attempts_limit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt number exceeds Assessment.attempts_limit';
    END IF;

    IF NEW.state IN ('submitted', 'corrected') AND NEW.submitted_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Submitted or corrected Attempt requires submitted_at';
    END IF;

    IF NEW.state <> 'corrected' AND NEW.score IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only corrected Attempt can store score';
    END IF;

    IF v_class_group IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM enroll_class_group
        WHERE id_student_user = NEW.id_student_user
          AND id_class_group = v_class_group
          AND (state = 'active' OR (v_assessment_state = 'completed' AND state = 'completed'));
    ELSEIF v_assessment_subject IS NOT NULL THEN
        SELECT COUNT(*)
        INTO v_exists
        FROM assessment_class_group acg
        JOIN class_group cg ON cg.id_class_group = acg.id_class_group
        JOIN enroll_class_group ecg
          ON ecg.id_class_group = cg.id_class_group
         AND ecg.id_student_user = NEW.id_student_user
         AND (ecg.state = 'active' OR (v_assessment_state = 'completed' AND ecg.state = 'completed'))
        WHERE acg.id_assessment = NEW.id_assessment
          AND cg.id_subject = v_assessment_subject;
    END IF;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active enrollment in the Assessment context';
    END IF;

    SELECT COUNT(*)
    INTO v_assessment_enrollment_exists
    FROM enroll_assessment
    WHERE id_student_user = NEW.id_student_user
      AND id_assessment = NEW.id_assessment
      AND state = 'active';

    IF v_assessment_enrollment_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attempt requires an active Assessment enrollment';
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

    IF v_question_type = 'single_choice' THEN
        SELECT COUNT(*)
        INTO v_selected_count
        FROM response_option
        WHERE id_response = NEW.id_response;

        IF v_selected_count >= 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Single-choice Questions allow at most one selected Option';
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

    IF NEW.status = 'justified' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Justified Attendance_Record must be produced by Absence_Justification processing';
    END IF;

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

DROP TRIGGER IF EXISTS bi_grade_sheet_validate$$
CREATE TRIGGER bi_grade_sheet_validate
BEFORE INSERT ON grade_sheet
FOR EACH ROW
BEGIN
    DECLARE v_matching_class_groups INT DEFAULT 0;

    IF NEW.scope = 'subject_occurrence' AND NEW.type <> 'final' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A Subject occurrence Grade_Sheet must be a final consolidated sheet';
    END IF;

    IF (NEW.scope = 'subject_occurrence'
            AND NOT (NEW.subject_occurrence_aggregate_id <=> NEW.id_course_occurrence))
       OR (NEW.scope = 'class_group'
            AND NEW.subject_occurrence_aggregate_id IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Grade_Sheet aggregate key must match its scope and Course occurrence';
    END IF;

    SELECT COUNT(*)
    INTO v_matching_class_groups
    FROM class_group
    WHERE id_subject = NEW.id_subject
      AND id_course_occurrence = NEW.id_course_occurrence;

    -- A class-group sheet is linked in the following insert into the
    -- association table, so it is briefly linkless inside the transaction.
    -- Only a consolidated sheet has no link by design and must therefore
    -- prove that its real class-group context already exists here.
    IF NEW.scope = 'subject_occurrence' AND v_matching_class_groups = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Grade_Sheet requires a real Class_Group in the same Subject occurrence';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_grade_sheet_validate$$
CREATE TRIGGER bu_grade_sheet_validate
BEFORE UPDATE ON grade_sheet
FOR EACH ROW
BEGIN
    DECLARE v_matching_class_groups INT DEFAULT 0;

    IF NEW.scope = 'subject_occurrence' AND NEW.type <> 'final' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A Subject occurrence Grade_Sheet must be a final consolidated sheet';
    END IF;

    IF (NEW.scope = 'subject_occurrence'
            AND NOT (NEW.subject_occurrence_aggregate_id <=> NEW.id_course_occurrence))
       OR (NEW.scope = 'class_group'
            AND NEW.subject_occurrence_aggregate_id IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Grade_Sheet aggregate key must match its scope and Course occurrence';
    END IF;

    SELECT COUNT(*)
    INTO v_matching_class_groups
    FROM class_group
    WHERE id_subject = NEW.id_subject
      AND id_course_occurrence = NEW.id_course_occurrence;

    IF NEW.scope = 'subject_occurrence' AND v_matching_class_groups = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'Grade_Sheet requires a real Class_Group in the same Subject occurrence';
    END IF;

    IF NEW.scope = 'subject_occurrence'
       AND EXISTS (
            SELECT 1
            FROM associate_grade_sheet_class_group
            WHERE id_grade_sheet = OLD.id_grade_sheet
       ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A consolidated Subject occurrence Grade_Sheet cannot be linked to a Class_Group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_associate_grade_sheet_class_group_validate$$
CREATE TRIGGER bi_associate_grade_sheet_class_group_validate
BEFORE INSERT ON associate_grade_sheet_class_group
FOR EACH ROW
BEGIN
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_grade_sheet_occurrence BIGINT UNSIGNED;
    DECLARE v_grade_sheet_scope VARCHAR(30);
    DECLARE v_grade_sheet_type VARCHAR(40);
    DECLARE v_class_group_subject BIGINT UNSIGNED;
    DECLARE v_class_group_occurrence BIGINT UNSIGNED;

    SELECT id_subject, id_course_occurrence, scope, type
    INTO v_grade_sheet_subject, v_grade_sheet_occurrence, v_grade_sheet_scope, v_grade_sheet_type
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    IF v_grade_sheet_scope <> 'class_group' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A consolidated Subject occurrence Grade_Sheet cannot be linked to a Class_Group';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM associate_grade_sheet_class_group
        WHERE id_grade_sheet = NEW.id_grade_sheet
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A Class_Group Grade_Sheet can belong to exactly one Class_Group';
    END IF;

    SELECT id_subject, id_course_occurrence
    INTO v_class_group_subject, v_class_group_occurrence
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF v_grade_sheet_subject <> v_class_group_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Sheet Class_Groups must belong to the same Subject';
    END IF;

    IF v_grade_sheet_occurrence <> v_class_group_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Sheet Class_Groups must belong to the same Course occurrence';
    END IF;

    IF v_grade_sheet_type = 'final'
       AND EXISTS (
            SELECT 1
            FROM associate_grade_sheet_class_group existing_link
            JOIN grade_sheet existing_sheet
              ON existing_sheet.id_grade_sheet = existing_link.id_grade_sheet
            WHERE existing_link.id_class_group = NEW.id_class_group
              AND existing_sheet.scope = 'class_group'
              AND existing_sheet.type = 'final'
              AND existing_sheet.id_grade_sheet <> NEW.id_grade_sheet
       ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A Class_Group can have only one final Grade_Sheet';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_associate_grade_sheet_class_group_validate$$
CREATE TRIGGER bu_associate_grade_sheet_class_group_validate
BEFORE UPDATE ON associate_grade_sheet_class_group
FOR EACH ROW
BEGIN
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_grade_sheet_occurrence BIGINT UNSIGNED;
    DECLARE v_grade_sheet_scope VARCHAR(30);
    DECLARE v_grade_sheet_type VARCHAR(40);
    DECLARE v_class_group_subject BIGINT UNSIGNED;
    DECLARE v_class_group_occurrence BIGINT UNSIGNED;

    SELECT id_subject, id_course_occurrence, scope, type
    INTO v_grade_sheet_subject, v_grade_sheet_occurrence, v_grade_sheet_scope, v_grade_sheet_type
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    IF v_grade_sheet_scope <> 'class_group' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A consolidated Subject occurrence Grade_Sheet cannot be linked to a Class_Group';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM associate_grade_sheet_class_group link_row
        WHERE link_row.id_grade_sheet = NEW.id_grade_sheet
          AND NOT (
                link_row.id_grade_sheet = OLD.id_grade_sheet
            AND link_row.id_class_group = OLD.id_class_group
          )
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A Class_Group Grade_Sheet can belong to exactly one Class_Group';
    END IF;

    SELECT id_subject, id_course_occurrence
    INTO v_class_group_subject, v_class_group_occurrence
    FROM class_group
    WHERE id_class_group = NEW.id_class_group;

    IF v_grade_sheet_subject <> v_class_group_subject THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Sheet Class_Groups must belong to the same Subject';
    END IF;

    IF v_grade_sheet_occurrence <> v_class_group_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Sheet Class_Groups must belong to the same Course occurrence';
    END IF;

    IF v_grade_sheet_type = 'final'
       AND EXISTS (
            SELECT 1
            FROM associate_grade_sheet_class_group existing_link
            JOIN grade_sheet existing_sheet
              ON existing_sheet.id_grade_sheet = existing_link.id_grade_sheet
            WHERE existing_link.id_class_group = NEW.id_class_group
              AND existing_sheet.scope = 'class_group'
              AND existing_sheet.type = 'final'
              AND existing_sheet.id_grade_sheet <> NEW.id_grade_sheet
       ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT =
            'A Class_Group can have only one final Grade_Sheet';
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
END$$

DROP TRIGGER IF EXISTS bu_based_on_assessment_validate$$
CREATE TRIGGER bu_based_on_assessment_validate
BEFORE UPDATE ON based_on_assessment
FOR EACH ROW
BEGIN
    DECLARE v_grade_sheet_subject BIGINT UNSIGNED;
    DECLARE v_assessment_subject BIGINT UNSIGNED;
    DECLARE v_block_subject BIGINT UNSIGNED;

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
    DECLARE v_sheet_max_grade DECIMAL(5,2);
    DECLARE v_max_grade DECIMAL(5,2);

    IF NEW.state IN ('draft', 'published', 'corrected') THEN
        SET NEW.active_student_user_id = NEW.id_user_student;
        IF NEW.cod_grade_record NOT LIKE 'AUTO-%'
                AND NEW.cod_grade_record NOT LIKE 'A-%' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Grade_Record must be automatically calculated';
        END IF;
    ELSE
        SET NEW.active_student_user_id = NULL;
    END IF;

    SELECT max_grade
    INTO v_sheet_max_grade
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    IF NEW.value > v_sheet_max_grade THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record value cannot exceed the Grade_Sheet max_grade';
    END IF;

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
    DECLARE v_sheet_max_grade DECIMAL(5,2);
    DECLARE v_max_grade DECIMAL(5,2);

    IF NEW.state IN ('draft', 'published', 'corrected') THEN
        SET NEW.active_student_user_id = NEW.id_user_student;
        IF NEW.cod_grade_record NOT LIKE 'AUTO-%'
                AND NEW.cod_grade_record NOT LIKE 'A-%' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active Grade_Record must be automatically calculated';
        END IF;
    ELSE
        SET NEW.active_student_user_id = NULL;
    END IF;

    SELECT max_grade
    INTO v_sheet_max_grade
    FROM grade_sheet
    WHERE id_grade_sheet = NEW.id_grade_sheet;

    IF NEW.value > v_sheet_max_grade THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Grade_Record value cannot exceed the Grade_Sheet max_grade';
    END IF;

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

END$$

DROP TRIGGER IF EXISTS bi_certificate_validate$$
CREATE TRIGGER bi_certificate_validate
BEFORE INSERT ON certificate
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;

    SELECT id_course
    INTO v_occurrence_course
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course occurrence must belong to the Certificate Course';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_certificate_validate$$
CREATE TRIGGER bu_certificate_validate
BEFORE UPDATE ON certificate
FOR EACH ROW
BEGIN
    DECLARE v_occurrence_course BIGINT UNSIGNED;

    SELECT id_course
    INTO v_occurrence_course
    FROM course_occurrence
    WHERE id_course_occurrence = NEW.id_course_occurrence;

    IF v_occurrence_course <> NEW.id_course THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course occurrence must belong to the Certificate Course';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_bgsc_validate$$
CREATE TRIGGER bi_bgsc_validate
BEFORE INSERT ON based_on_grade_sheet_certificate
FOR EACH ROW
BEGIN
    DECLARE v_course BIGINT UNSIGNED;
    DECLARE v_certificate_occurrence BIGINT UNSIGNED;
    DECLARE v_subject BIGINT UNSIGNED;
    DECLARE v_grade_sheet_occurrence BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT c.id_course, c.id_course_occurrence, gs.id_subject, gs.id_course_occurrence
    INTO v_course, v_certificate_occurrence, v_subject, v_grade_sheet_occurrence
    FROM certificate c
    JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
    WHERE c.id_certificate = NEW.id_certificate;

    IF v_certificate_occurrence <> v_grade_sheet_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Grade_Sheet must belong to the same Course occurrence';
    END IF;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = v_course
      AND id_subject = v_subject;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course must integrate the Subject of the referenced Grade_Sheet';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_bgsc_validate$$
CREATE TRIGGER bu_bgsc_validate
BEFORE UPDATE ON based_on_grade_sheet_certificate
FOR EACH ROW
BEGIN
    DECLARE v_course BIGINT UNSIGNED;
    DECLARE v_certificate_occurrence BIGINT UNSIGNED;
    DECLARE v_subject BIGINT UNSIGNED;
    DECLARE v_grade_sheet_occurrence BIGINT UNSIGNED;
    DECLARE v_exists INT DEFAULT 0;

    SELECT c.id_course, c.id_course_occurrence, gs.id_subject, gs.id_course_occurrence
    INTO v_course, v_certificate_occurrence, v_subject, v_grade_sheet_occurrence
    FROM certificate c
    JOIN grade_sheet gs ON gs.id_grade_sheet = NEW.id_grade_sheet
    WHERE c.id_certificate = NEW.id_certificate;

    IF v_certificate_occurrence <> v_grade_sheet_occurrence THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Grade_Sheet must belong to the same Course occurrence';
    END IF;

    SELECT COUNT(*)
    INTO v_exists
    FROM integrate_subject
    WHERE id_course = v_course
      AND id_subject = v_subject;

    IF v_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Certificate Course must integrate the Subject of the referenced Grade_Sheet';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_registered_direct_channel_validate$$
CREATE TRIGGER bu_registered_direct_channel_validate
BEFORE UPDATE ON channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = OLD.id_channel
    ) AND (
        NEW.id_channel <> OLD.id_channel
        OR NEW.state <> 'active'
        OR NEW.type <> 'message'
        OR NEW.visibility <> 'participants'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registered direct-message channels must remain active message channels for participants';
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_registered_direct_channel_validate$$
CREATE TRIGGER bd_registered_direct_channel_validate
BEFORE DELETE ON channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = OLD.id_channel
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unregister a direct-message channel before deleting it';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_direct_participation_validate$$
CREATE TRIGGER bi_direct_participation_validate
BEFORE INSERT ON participate_channel
FOR EACH ROW
BEGIN
    IF NEW.state = 'active' AND EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = NEW.id_channel
          AND NEW.id_user NOT IN (dmc.id_user_low, dmc.id_user_high)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registered direct-message channels cannot have a third active participant';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_direct_participation_validate$$
CREATE TRIGGER bu_direct_participation_validate
BEFORE UPDATE ON participate_channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = OLD.id_channel
          AND OLD.id_user IN (dmc.id_user_low, dmc.id_user_high)
    ) AND (
        NEW.id_channel <> OLD.id_channel
        OR NEW.id_user <> OLD.id_user
        OR NEW.state <> 'active'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required direct-message participants must remain active';
    END IF;

    IF NEW.state = 'active' AND EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = NEW.id_channel
          AND NEW.id_user NOT IN (dmc.id_user_low, dmc.id_user_high)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registered direct-message channels cannot have a third active participant';
    END IF;
END$$

DROP TRIGGER IF EXISTS bd_direct_participation_validate$$
CREATE TRIGGER bd_direct_participation_validate
BEFORE DELETE ON participate_channel
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM direct_message_channel dmc
        WHERE dmc.id_channel = OLD.id_channel
          AND OLD.id_user IN (dmc.id_user_low, dmc.id_user_high)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Required direct-message participants cannot be removed';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_direct_channel_class_group_validate$$
CREATE TRIGGER bi_direct_channel_class_group_validate
BEFORE INSERT ON associate_channel_class_group
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a class group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_direct_channel_class_group_validate$$
CREATE TRIGGER bu_direct_channel_class_group_validate
BEFORE UPDATE ON associate_channel_class_group
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a class group';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_direct_channel_content_block_validate$$
CREATE TRIGGER bi_direct_channel_content_block_validate
BEFORE INSERT ON associate_channel_content_block
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a content block';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_direct_channel_content_block_validate$$
CREATE TRIGGER bu_direct_channel_content_block_validate
BEFORE UPDATE ON associate_channel_content_block
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with a content block';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_direct_channel_assessment_validate$$
CREATE TRIGGER bi_direct_channel_assessment_validate
BEFORE INSERT ON associate_channel_assessment
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with an assessment';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_direct_channel_assessment_validate$$
CREATE TRIGGER bu_direct_channel_assessment_validate
BEFORE UPDATE ON associate_channel_assessment
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM direct_message_channel dmc WHERE dmc.id_channel = NEW.id_channel) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message channels cannot be associated with an assessment';
    END IF;
END$$

DROP TRIGGER IF EXISTS bi_direct_message_channel_validate$$
CREATE TRIGGER bi_direct_message_channel_validate
BEFORE INSERT ON direct_message_channel
FOR EACH ROW
BEGIN
    DECLARE v_valid_channel INT DEFAULT 0;

    IF NEW.id_user_low >= NEW.id_user_high THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Direct message participants must be stored in ascending order';
    END IF;

    SELECT COUNT(*)
    INTO v_valid_channel
    FROM channel c
    WHERE c.id_channel = NEW.id_channel
      AND c.state = 'active'
      AND c.type = 'message'
      AND c.visibility = 'participants'
      AND (SELECT COUNT(*) FROM participate_channel pc
           WHERE pc.id_channel = c.id_channel AND pc.state = 'active') = 2
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_low AND pc.state = 'active')
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_high AND pc.state = 'active')
      AND NOT EXISTS (SELECT 1 FROM associate_channel_class_group acg WHERE acg.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_content_block acb WHERE acb.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_assessment aa WHERE aa.id_channel = c.id_channel);

    IF v_valid_channel = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message registration requires one active context-free channel with exactly both participants';
    END IF;
END$$

DROP TRIGGER IF EXISTS bu_direct_message_channel_validate$$
CREATE TRIGGER bu_direct_message_channel_validate
BEFORE UPDATE ON direct_message_channel
FOR EACH ROW
BEGIN
    DECLARE v_valid_channel INT DEFAULT 0;

    IF NEW.id_user_low >= NEW.id_user_high THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Direct message participants must be stored in ascending order';
    END IF;

    SELECT COUNT(*)
    INTO v_valid_channel
    FROM channel c
    WHERE c.id_channel = NEW.id_channel
      AND c.state = 'active'
      AND c.type = 'message'
      AND c.visibility = 'participants'
      AND (SELECT COUNT(*) FROM participate_channel pc
           WHERE pc.id_channel = c.id_channel AND pc.state = 'active') = 2
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_low AND pc.state = 'active')
      AND EXISTS (SELECT 1 FROM participate_channel pc
                  WHERE pc.id_channel = c.id_channel AND pc.id_user = NEW.id_user_high AND pc.state = 'active')
      AND NOT EXISTS (SELECT 1 FROM associate_channel_class_group acg WHERE acg.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_content_block acb WHERE acb.id_channel = c.id_channel)
      AND NOT EXISTS (SELECT 1 FROM associate_channel_assessment aa WHERE aa.id_channel = c.id_channel);

    IF v_valid_channel = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Direct-message registration requires one active context-free channel with exactly both participants';
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
-- Constraints that must remain in the Service layer (not fully SQL):
-- - profile authorizations in process/approval operations;
-- - cross-context consistency (course/subject/class group/block);
-- - temporal limits with advanced business rules;
-- - "pelo menos um" em relacoes opcionais do lado pai;
-- - message routing/destination rules.
-- ---------------------------------------------------------
