-- Removing a subject from a course closes the association for future work.
-- Existing class groups and all dependent academic records retain their context.
SET @gape_has_integrate_subject_state = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'integrate_subject'
      AND column_name = 'state'
);
SET @gape_integrate_subject_state_sql = IF(
    @gape_has_integrate_subject_state = 0,
    'ALTER TABLE integrate_subject ADD COLUMN state VARCHAR(20) NOT NULL DEFAULT ''active'' AFTER mandatory',
    'SELECT 1'
);
PREPARE gape_integrate_subject_state_statement FROM @gape_integrate_subject_state_sql;
EXECUTE gape_integrate_subject_state_statement;
DEALLOCATE PREPARE gape_integrate_subject_state_statement;

SET @gape_has_integrate_subject_ended_at = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'integrate_subject'
      AND column_name = 'ended_at'
);
SET @gape_integrate_subject_ended_at_sql = IF(
    @gape_has_integrate_subject_ended_at = 0,
    'ALTER TABLE integrate_subject ADD COLUMN ended_at DATE NULL AFTER state',
    'SELECT 1'
);
PREPARE gape_integrate_subject_ended_at_statement FROM @gape_integrate_subject_ended_at_sql;
EXECUTE gape_integrate_subject_ended_at_statement;
DEALLOCATE PREPARE gape_integrate_subject_ended_at_statement;

UPDATE integrate_subject
SET state = 'active', ended_at = NULL
WHERE state IS NULL OR state NOT IN ('active', 'historical');

SET @gape_has_integrate_subject_state_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'integrate_subject'
      AND index_name = 'idx_integrate_subject_state'
);
SET @gape_integrate_subject_state_index_sql = IF(
    @gape_has_integrate_subject_state_index = 0,
    'ALTER TABLE integrate_subject ADD KEY idx_integrate_subject_state (state)',
    'SELECT 1'
);
PREPARE gape_integrate_subject_state_index_statement FROM @gape_integrate_subject_state_index_sql;
EXECUTE gape_integrate_subject_state_index_statement;
DEALLOCATE PREPARE gape_integrate_subject_state_index_statement;
