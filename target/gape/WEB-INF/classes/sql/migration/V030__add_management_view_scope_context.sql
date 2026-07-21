-- Management views existed in the baseline only as a title, type and coarse
-- visibility label.  A scope without its target is ambiguous and cannot be
-- authorized safely, so phase 15 persists an explicit context and owner.
--
-- sql/schema.sql is the current schema.  The guards keep this migration valid
-- both for a fresh bootstrap (where the columns already exist) and for an
-- installed version-29 database.
DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_management_view_scope_context$$
CREATE PROCEDURE migrate_management_view_scope_context()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'management_view'
          AND column_name = 'scope_target_type'
    ) THEN
        ALTER TABLE management_view
            ADD COLUMN scope_target_type VARCHAR(40) NULL AFTER visibility_scope;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'management_view'
          AND column_name = 'scope_target_id'
    ) THEN
        ALTER TABLE management_view
            ADD COLUMN scope_target_id BIGINT UNSIGNED NULL AFTER scope_target_type;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'management_view'
          AND column_name = 'owner_user_id'
    ) THEN
        ALTER TABLE management_view
            ADD COLUMN owner_user_id BIGINT UNSIGNED NULL AFTER scope_target_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'management_view'
          AND index_name = 'idx_management_view_scope_target'
    ) THEN
        ALTER TABLE management_view
            ADD INDEX idx_management_view_scope_target (
                visibility_scope, scope_target_type, scope_target_id, state
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'management_view'
          AND index_name = 'idx_management_view_owner'
    ) THEN
        ALTER TABLE management_view
            ADD INDEX idx_management_view_owner (owner_user_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'management_view'
          AND constraint_name = 'fk_management_view_owner'
    ) THEN
        -- No explicit referential action: owner_user_id also participates in
        -- the PERSONAL scope CHECK.
        ALTER TABLE management_view
            ADD CONSTRAINT fk_management_view_owner
                FOREIGN KEY (owner_user_id) REFERENCES user_account (id_user);
    END IF;
END$$
CALL migrate_management_view_scope_context()$$
DROP PROCEDURE migrate_management_view_scope_context$$
DELIMITER ;

-- Normalize the two deliberately permissive legacy values before checks are
-- introduced.  An old grant is useful as an owner hint but never as a scope
-- authorization decision.
UPDATE management_view
SET state = CASE LOWER(TRIM(state))
        WHEN 'active' THEN 'active'
        WHEN 'inactive' THEN 'inactive'
        WHEN 'archived' THEN 'archived'
        ELSE 'archived'
    END;

UPDATE management_view
SET visibility_scope = CASE LOWER(TRIM(visibility_scope))
        WHEN 'global' THEN 'global'
        WHEN 'organization' THEN 'organization'
        WHEN 'course' THEN 'course'
        WHEN 'subject' THEN 'subject'
        WHEN 'class_group' THEN 'class_group'
        WHEN 'personal' THEN 'personal'
        WHEN 'user' THEN 'personal'
        ELSE 'global'
    END,
    type = CASE LOWER(TRIM(type))
        WHEN 'dashboard' THEN 'dashboard'
        WHEN 'report' THEN 'report'
        WHEN 'control_panel' THEN 'control_panel'
        WHEN 'other' THEN 'other'
        WHEN 'analytics' THEN 'other'
        ELSE 'other'
    END;

UPDATE management_view mv
JOIN (
    SELECT id_management_view, MIN(id_user) AS owner_user_id
    FROM access_management_view
    GROUP BY id_management_view
) explicit_grant ON explicit_grant.id_management_view = mv.id_management_view
SET mv.owner_user_id = explicit_grant.owner_user_id
WHERE mv.owner_user_id IS NULL;

UPDATE management_view
SET scope_target_type = NULL,
    scope_target_id = NULL
WHERE visibility_scope = 'global';

UPDATE management_view
SET scope_target_type = 'ORGANIZATION'
WHERE visibility_scope = 'organization';

UPDATE management_view
SET scope_target_type = 'COURSE'
WHERE visibility_scope = 'course';

UPDATE management_view
SET scope_target_type = 'SUBJECT'
WHERE visibility_scope = 'subject';

UPDATE management_view
SET scope_target_type = 'CLASS_GROUP'
WHERE visibility_scope = 'class_group';

UPDATE management_view
SET scope_target_type = 'USER',
    scope_target_id = owner_user_id
WHERE visibility_scope = 'personal'
  AND owner_user_id IS NOT NULL;

-- There is no reliable way to infer a missing historical scope target.  Keep
-- the record for audit/history, but archive it as a target-free global record
-- rather than accidentally exposing it in somebody else's context.
UPDATE management_view
SET visibility_scope = 'global',
    scope_target_type = NULL,
    scope_target_id = NULL,
    state = 'archived'
WHERE visibility_scope <> 'global'
  AND (scope_target_id IS NULL OR scope_target_id = 0);

UPDATE management_view mv
LEFT JOIN organization target ON target.id_organization = mv.scope_target_id
SET mv.visibility_scope = 'global',
    mv.scope_target_type = NULL,
    mv.scope_target_id = NULL,
    mv.state = 'archived'
WHERE mv.visibility_scope = 'organization'
  AND target.id_organization IS NULL;

UPDATE management_view mv
LEFT JOIN course target ON target.id_course = mv.scope_target_id
SET mv.visibility_scope = 'global',
    mv.scope_target_type = NULL,
    mv.scope_target_id = NULL,
    mv.state = 'archived'
WHERE mv.visibility_scope = 'course'
  AND target.id_course IS NULL;

UPDATE management_view mv
LEFT JOIN subject target ON target.id_subject = mv.scope_target_id
SET mv.visibility_scope = 'global',
    mv.scope_target_type = NULL,
    mv.scope_target_id = NULL,
    mv.state = 'archived'
WHERE mv.visibility_scope = 'subject'
  AND target.id_subject IS NULL;

UPDATE management_view mv
LEFT JOIN class_group target ON target.id_class_group = mv.scope_target_id
SET mv.visibility_scope = 'global',
    mv.scope_target_type = NULL,
    mv.scope_target_id = NULL,
    mv.state = 'archived'
WHERE mv.visibility_scope = 'class_group'
  AND target.id_class_group IS NULL;

UPDATE management_view mv
LEFT JOIN user_account target ON target.id_user = mv.scope_target_id
SET mv.visibility_scope = 'global',
    mv.scope_target_type = NULL,
    mv.scope_target_id = NULL,
    mv.state = 'archived'
WHERE mv.visibility_scope = 'personal'
  AND (target.id_user IS NULL OR mv.owner_user_id IS NULL OR mv.scope_target_id <> mv.owner_user_id);

DELIMITER $$
DROP PROCEDURE IF EXISTS add_management_view_scope_constraints$$
CREATE PROCEDURE add_management_view_scope_constraints()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'management_view'
          AND constraint_name = 'ck_management_view_type'
    ) THEN
        ALTER TABLE management_view
            ADD CONSTRAINT ck_management_view_type
                CHECK (type IN ('dashboard', 'report', 'control_panel', 'other'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'management_view'
          AND constraint_name = 'ck_management_view_scope'
    ) THEN
        ALTER TABLE management_view
            ADD CONSTRAINT ck_management_view_scope
                CHECK (visibility_scope IN ('global', 'organization', 'course', 'subject', 'class_group', 'personal'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'management_view'
          AND constraint_name = 'ck_management_view_state'
    ) THEN
        ALTER TABLE management_view
            ADD CONSTRAINT ck_management_view_state
                CHECK (state IN ('active', 'inactive', 'archived'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'management_view'
          AND constraint_name = 'ck_management_view_scope_context'
    ) THEN
        ALTER TABLE management_view
            ADD CONSTRAINT ck_management_view_scope_context
                CHECK (
                    (visibility_scope = 'global'
                        AND scope_target_type IS NULL
                        AND scope_target_id IS NULL)
                    OR (visibility_scope = 'organization'
                        AND scope_target_type = 'ORGANIZATION'
                        AND scope_target_id IS NOT NULL
                        AND scope_target_id > 0)
                    OR (visibility_scope = 'course'
                        AND scope_target_type = 'COURSE'
                        AND scope_target_id IS NOT NULL
                        AND scope_target_id > 0)
                    OR (visibility_scope = 'subject'
                        AND scope_target_type = 'SUBJECT'
                        AND scope_target_id IS NOT NULL
                        AND scope_target_id > 0)
                    OR (visibility_scope = 'class_group'
                        AND scope_target_type = 'CLASS_GROUP'
                        AND scope_target_id IS NOT NULL
                        AND scope_target_id > 0)
                    OR (visibility_scope = 'personal'
                        AND scope_target_type = 'USER'
                        AND scope_target_id IS NOT NULL
                        AND owner_user_id IS NOT NULL
                        AND scope_target_id = owner_user_id)
                );
    END IF;
END$$
CALL add_management_view_scope_constraints()$$
DROP PROCEDURE add_management_view_scope_constraints$$
DELIMITER ;
