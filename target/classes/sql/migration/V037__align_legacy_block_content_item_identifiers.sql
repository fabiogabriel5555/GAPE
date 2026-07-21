-- Content created before block-scoped pedagogical IDs was introduced could
-- receive a low global AUTO_INCREMENT value (for example, "PDF 2").  Move
-- only late-created, single-block content that currently sorts before the
-- block's lessons/assessments.  Older content that was created before the
-- block activities remains the first item, preserving the original fixture
-- order.

CREATE TEMPORARY TABLE pedagogical_content_id_map (
    old_id BIGINT UNSIGNED NOT NULL,
    new_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (old_id),
    UNIQUE KEY uq_pedagogical_content_new_id (new_id)
);

INSERT INTO pedagogical_content_id_map (old_id, new_id)
WITH block_activity_limits AS (
    SELECT
        association.id_content_item,
        association.id_content_block,
        GREATEST(
            COALESCE((SELECT MAX(lesson.id_lesson)
                      FROM lesson
                      WHERE lesson.id_content_block = association.id_content_block), 0),
            COALESCE((SELECT MAX(assessment.id_assessment)
                      FROM assessment
                      WHERE assessment.id_content_block = association.id_content_block), 0),
            COALESCE((SELECT MAX(other_association.id_content_item)
                      FROM associate_block_content other_association
                      WHERE other_association.id_content_block = association.id_content_block), 0)
        ) AS block_max_id,
        GREATEST(
            COALESCE((SELECT MAX(lesson.starts_at)
                      FROM lesson
                      WHERE lesson.id_content_block = association.id_content_block), '1000-01-01 00:00:00'),
            COALESCE((SELECT MAX(assessment.available_from)
                      FROM assessment
                      WHERE assessment.id_content_block = association.id_content_block), '1000-01-01 00:00:00')
        ) AS latest_activity_start
    FROM associate_block_content association
), candidates AS (
    SELECT
        content.id_content_item AS old_id,
        ROW_NUMBER() OVER (ORDER BY content.created_at, content.id_content_item) AS sequence_no
    FROM content_item content
    JOIN block_activity_limits limits
      ON limits.id_content_item = content.id_content_item
    WHERE content.id_content_item < limits.block_max_id
      AND content.created_at > limits.latest_activity_start
      AND NOT EXISTS (
          SELECT 1
          FROM associate_block_content duplicate_association
          WHERE duplicate_association.id_content_item = content.id_content_item
            AND duplicate_association.id_content_block <> limits.id_content_block
      )
)
SELECT
    candidates.old_id,
    GREATEST(
        COALESCE((SELECT MAX(id_content_item) FROM content_item), 0),
        COALESCE((SELECT MAX(id_lesson) FROM lesson), 0),
        COALESCE((SELECT MAX(id_assessment) FROM assessment), 0)
    ) + candidates.sequence_no
FROM candidates;

SET FOREIGN_KEY_CHECKS = 0;

UPDATE content_file file_row
JOIN pedagogical_content_id_map mapping ON mapping.old_id = file_row.id_content_item
SET file_row.id_content_item = mapping.new_id;

UPDATE associate_organization_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE associate_organic_unit_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE associate_course_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE associate_subject_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE associate_class_group_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE associate_block_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE associate_assessment_content association
JOIN pedagogical_content_id_map mapping ON mapping.old_id = association.id_content_item
SET association.id_content_item = mapping.new_id;

UPDATE content_item content
JOIN pedagogical_content_id_map mapping ON mapping.old_id = content.id_content_item
SET content.id_content_item = mapping.new_id;

SET FOREIGN_KEY_CHECKS = 1;

DROP TEMPORARY TABLE pedagogical_content_id_map;
