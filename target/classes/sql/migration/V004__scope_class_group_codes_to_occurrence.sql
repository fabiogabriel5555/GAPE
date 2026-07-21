DROP PROCEDURE IF EXISTS gape_migrate_v004;

DELIMITER $$
CREATE PROCEDURE gape_migrate_v004()
BEGIN
    DECLARE v_count BIGINT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM (
        SELECT id_course_occurrence, id_subject, cod_class_group
        FROM class_group
        GROUP BY id_course_occurrence, id_subject, cod_class_group
        HAVING COUNT(*) > 1
    ) duplicates;

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Class-group migration stopped: duplicate occurrence/subject/code rows require manual resolution';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_group'
          AND index_name = 'idx_class_group_subject'
    ) THEN
        ALTER TABLE class_group ADD KEY idx_class_group_subject (id_subject);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_group'
          AND index_name = 'uq_class_group_subject_code'
    ) THEN
        ALTER TABLE class_group DROP INDEX uq_class_group_subject_code;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'class_group'
          AND index_name = 'uq_class_group_occurrence_subject_code'
          AND non_unique = 0
    ) THEN
        ALTER TABLE class_group
            ADD UNIQUE KEY uq_class_group_occurrence_subject_code (
                id_course_occurrence,
                id_subject,
                cod_class_group
            );
    END IF;
END$$
DELIMITER ;

CALL gape_migrate_v004();
DROP PROCEDURE IF EXISTS gape_migrate_v004;
