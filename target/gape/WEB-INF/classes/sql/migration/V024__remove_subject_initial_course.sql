-- A subject is an organizational learning unit and may be created before it
-- receives any curricular placement.  Course associations remain independent,
-- historical records and may all be closed without deleting the subject.
SET @gape_has_subject_initial_course_fk = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'subject'
      AND constraint_name = 'fk_subject_initial_course'
      AND constraint_type = 'FOREIGN KEY'
);
SET @gape_drop_subject_initial_course_fk_sql = IF(
    @gape_has_subject_initial_course_fk > 0,
    'ALTER TABLE subject DROP FOREIGN KEY fk_subject_initial_course',
    'SELECT 1'
);
PREPARE gape_drop_subject_initial_course_fk_statement FROM @gape_drop_subject_initial_course_fk_sql;
EXECUTE gape_drop_subject_initial_course_fk_statement;
DEALLOCATE PREPARE gape_drop_subject_initial_course_fk_statement;

SET @gape_has_subject_initial_course_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'subject'
      AND index_name = 'idx_subject_initial_course'
);
SET @gape_drop_subject_initial_course_index_sql = IF(
    @gape_has_subject_initial_course_index > 0,
    'ALTER TABLE subject DROP INDEX idx_subject_initial_course',
    'SELECT 1'
);
PREPARE gape_drop_subject_initial_course_index_statement FROM @gape_drop_subject_initial_course_index_sql;
EXECUTE gape_drop_subject_initial_course_index_statement;
DEALLOCATE PREPARE gape_drop_subject_initial_course_index_statement;

SET @gape_has_subject_initial_course = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'subject'
      AND column_name = 'initial_course_id'
);
SET @gape_drop_subject_initial_course_sql = IF(
    @gape_has_subject_initial_course > 0,
    'ALTER TABLE subject DROP COLUMN initial_course_id',
    'SELECT 1'
);
PREPARE gape_drop_subject_initial_course_statement FROM @gape_drop_subject_initial_course_sql;
EXECUTE gape_drop_subject_initial_course_statement;
DEALLOCATE PREPARE gape_drop_subject_initial_course_statement;
