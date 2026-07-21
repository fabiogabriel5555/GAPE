-- A subject-occurrence grade sheet is not created from a course association.
-- It exists only as the single consolidated result of real class groups in the
-- same subject/course-occurrence context.  The scope marker gives the
-- database a durable way to enforce that distinction and its uniqueness.
DELIMITER $$
DROP PROCEDURE IF EXISTS gape_add_grade_sheet_scope_columns$$
CREATE PROCEDURE gape_add_grade_sheet_scope_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'grade_sheet'
          AND column_name = 'scope'
    ) THEN
        ALTER TABLE grade_sheet
            ADD COLUMN scope VARCHAR(30) NULL AFTER state;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'grade_sheet'
          AND column_name = 'subject_occurrence_aggregate_id'
    ) THEN
        ALTER TABLE grade_sheet
            ADD COLUMN subject_occurrence_aggregate_id BIGINT UNSIGNED NULL AFTER scope;
    END IF;
END$$
CALL gape_add_grade_sheet_scope_columns()$$
DROP PROCEDURE gape_add_grade_sheet_scope_columns$$
DELIMITER ;

-- A grade sheet without a class-group link is valid only when it is the final
-- aggregate for a real subject occurrence.  Discard artificial sheets and
-- surplus aggregates rather than retaining incompatible historical residue.
CREATE TEMPORARY TABLE gape_invalid_subject_occurrence_grade_sheet (
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_grade_sheet)
);

INSERT INTO gape_invalid_subject_occurrence_grade_sheet (id_grade_sheet)
SELECT grade_sheet_row.id_grade_sheet
FROM grade_sheet grade_sheet_row
WHERE NOT EXISTS (
        SELECT 1
        FROM associate_grade_sheet_class_group class_group_link
        WHERE class_group_link.id_grade_sheet = grade_sheet_row.id_grade_sheet
    )
  AND (
        grade_sheet_row.type <> 'final'
        OR NOT EXISTS (
            SELECT 1
            FROM class_group class_group_row
            WHERE class_group_row.id_subject = grade_sheet_row.id_subject
              AND class_group_row.id_course_occurrence = grade_sheet_row.id_course_occurrence
        )
        OR EXISTS (
            SELECT 1
            FROM grade_sheet prior_aggregate
            WHERE prior_aggregate.id_subject = grade_sheet_row.id_subject
              AND prior_aggregate.id_course_occurrence = grade_sheet_row.id_course_occurrence
              AND prior_aggregate.type = 'final'
              AND prior_aggregate.id_grade_sheet < grade_sheet_row.id_grade_sheet
              AND NOT EXISTS (
                    SELECT 1
                    FROM associate_grade_sheet_class_group prior_link
                    WHERE prior_link.id_grade_sheet = prior_aggregate.id_grade_sheet
              )
        )
    );

UPDATE certificate certificate_row
JOIN based_on_grade_sheet_certificate certificate_grade_sheet
  ON certificate_grade_sheet.id_certificate = certificate_row.id_certificate
JOIN gape_invalid_subject_occurrence_grade_sheet invalid_sheet
  ON invalid_sheet.id_grade_sheet = certificate_grade_sheet.id_grade_sheet
SET certificate_row.state = 'draft',
    certificate_row.validation_code = NULL,
    certificate_row.issued_at = NULL,
    certificate_row.final_grade = NULL;

DELETE certificate_grade_sheet
FROM based_on_grade_sheet_certificate certificate_grade_sheet
JOIN gape_invalid_subject_occurrence_grade_sheet invalid_sheet
  ON invalid_sheet.id_grade_sheet = certificate_grade_sheet.id_grade_sheet;

DELETE grade_record_row
FROM grade_record grade_record_row
JOIN gape_invalid_subject_occurrence_grade_sheet invalid_sheet
  ON invalid_sheet.id_grade_sheet = grade_record_row.id_grade_sheet;

DELETE grade_sheet_row
FROM grade_sheet grade_sheet_row
JOIN gape_invalid_subject_occurrence_grade_sheet invalid_sheet
  ON invalid_sheet.id_grade_sheet = grade_sheet_row.id_grade_sheet;

DROP TEMPORARY TABLE gape_invalid_subject_occurrence_grade_sheet;

-- Existing class-group sheets are normalized to a single class group.  A
-- final sheet duplicated for a group is surplus: the lowest identifier is
-- retained and conformance later recreates any genuinely missing source.
CREATE TEMPORARY TABLE gape_extra_grade_sheet_class_group_link (
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    id_class_group BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_grade_sheet, id_class_group)
);

INSERT INTO gape_extra_grade_sheet_class_group_link (id_grade_sheet, id_class_group)
SELECT class_group_link.id_grade_sheet, class_group_link.id_class_group
FROM associate_grade_sheet_class_group class_group_link
JOIN (
    SELECT id_grade_sheet, MIN(id_class_group) AS retained_class_group_id
    FROM associate_grade_sheet_class_group
    GROUP BY id_grade_sheet
    HAVING COUNT(*) > 1
) multiple_links
  ON multiple_links.id_grade_sheet = class_group_link.id_grade_sheet
WHERE class_group_link.id_class_group <> multiple_links.retained_class_group_id;

DELETE class_group_link
FROM associate_grade_sheet_class_group class_group_link
JOIN gape_extra_grade_sheet_class_group_link extra_link
  ON extra_link.id_grade_sheet = class_group_link.id_grade_sheet
 AND extra_link.id_class_group = class_group_link.id_class_group;

DROP TEMPORARY TABLE gape_extra_grade_sheet_class_group_link;

UPDATE grade_sheet grade_sheet_row
SET scope = CASE
        WHEN EXISTS (
            SELECT 1
            FROM associate_grade_sheet_class_group class_group_link
            WHERE class_group_link.id_grade_sheet = grade_sheet_row.id_grade_sheet
        ) THEN 'class_group'
        ELSE 'subject_occurrence'
    END,
    subject_occurrence_aggregate_id = CASE
        WHEN EXISTS (
            SELECT 1
            FROM associate_grade_sheet_class_group class_group_link
            WHERE class_group_link.id_grade_sheet = grade_sheet_row.id_grade_sheet
        ) THEN NULL
        ELSE grade_sheet_row.id_course_occurrence
    END;

CREATE TEMPORARY TABLE gape_duplicate_final_class_group_grade_sheet (
    id_grade_sheet BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id_grade_sheet)
);

INSERT INTO gape_duplicate_final_class_group_grade_sheet (id_grade_sheet)
SELECT grade_sheet_row.id_grade_sheet
FROM grade_sheet grade_sheet_row
JOIN associate_grade_sheet_class_group class_group_link
  ON class_group_link.id_grade_sheet = grade_sheet_row.id_grade_sheet
WHERE grade_sheet_row.scope = 'class_group'
  AND grade_sheet_row.type = 'final'
  AND EXISTS (
        SELECT 1
        FROM grade_sheet prior_sheet
        JOIN associate_grade_sheet_class_group prior_link
          ON prior_link.id_grade_sheet = prior_sheet.id_grade_sheet
        WHERE prior_link.id_class_group = class_group_link.id_class_group
          AND prior_sheet.scope = 'class_group'
          AND prior_sheet.type = 'final'
          AND prior_sheet.id_grade_sheet < grade_sheet_row.id_grade_sheet
    );

UPDATE certificate certificate_row
JOIN based_on_grade_sheet_certificate certificate_grade_sheet
  ON certificate_grade_sheet.id_certificate = certificate_row.id_certificate
JOIN gape_duplicate_final_class_group_grade_sheet duplicate_sheet
  ON duplicate_sheet.id_grade_sheet = certificate_grade_sheet.id_grade_sheet
SET certificate_row.state = 'draft',
    certificate_row.validation_code = NULL,
    certificate_row.issued_at = NULL,
    certificate_row.final_grade = NULL;

DELETE certificate_grade_sheet
FROM based_on_grade_sheet_certificate certificate_grade_sheet
JOIN gape_duplicate_final_class_group_grade_sheet duplicate_sheet
  ON duplicate_sheet.id_grade_sheet = certificate_grade_sheet.id_grade_sheet;

DELETE grade_record_row
FROM grade_record grade_record_row
JOIN gape_duplicate_final_class_group_grade_sheet duplicate_sheet
  ON duplicate_sheet.id_grade_sheet = grade_record_row.id_grade_sheet;

DELETE grade_sheet_row
FROM grade_sheet grade_sheet_row
JOIN gape_duplicate_final_class_group_grade_sheet duplicate_sheet
  ON duplicate_sheet.id_grade_sheet = grade_sheet_row.id_grade_sheet;

DROP TEMPORARY TABLE gape_duplicate_final_class_group_grade_sheet;

DELIMITER $$
DROP PROCEDURE IF EXISTS gape_finalize_grade_sheet_scope$$
CREATE PROCEDURE gape_finalize_grade_sheet_scope()
BEGIN
    ALTER TABLE grade_sheet
        MODIFY COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'class_group';

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'grade_sheet'
          AND constraint_name = 'ck_grade_sheet_scope'
    ) THEN
        ALTER TABLE grade_sheet
            ADD CONSTRAINT ck_grade_sheet_scope
            CHECK (scope IN ('class_group', 'subject_occurrence'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'grade_sheet'
          AND index_name = 'uq_grade_sheet_subject_occurrence_aggregate'
    ) THEN
        ALTER TABLE grade_sheet
            ADD UNIQUE KEY uq_grade_sheet_subject_occurrence_aggregate (
                id_subject, subject_occurrence_aggregate_id
            );
    END IF;
END$$
CALL gape_finalize_grade_sheet_scope()$$
DROP PROCEDURE gape_finalize_grade_sheet_scope$$
DELIMITER ;

DELIMITER $$
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

    -- Class-group sheets are linked immediately after insertion.  A
    -- consolidated sheet is intentionally linkless, so only it must prove
    -- its context at this point.
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
DELIMITER ;
