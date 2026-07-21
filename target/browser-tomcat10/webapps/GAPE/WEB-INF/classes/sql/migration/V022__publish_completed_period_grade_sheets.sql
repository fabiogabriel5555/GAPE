DELIMITER $$
DROP PROCEDURE IF EXISTS gape_add_grade_sheet_publication_explanation$$
CREATE PROCEDURE gape_add_grade_sheet_publication_explanation()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'grade_sheet'
          AND column_name = 'publication_explanation'
    ) THEN
        ALTER TABLE grade_sheet
            ADD COLUMN publication_explanation VARCHAR(1000) NULL AFTER released_at;
    END IF;
END$$
CALL gape_add_grade_sheet_publication_explanation()$$
DROP PROCEDURE gape_add_grade_sheet_publication_explanation$$
DELIMITER ;

-- A completed academic period makes its grade sheets available even when
-- assessment information is still pending.  The lifecycle service refines
-- this fallback text into the precise pending reasons immediately after the
-- migration/startup conformance pass.
UPDATE grade_sheet gs
SET state = 'published',
    released_at = COALESCE(released_at, CURRENT_TIMESTAMP),
    publication_explanation = COALESCE(
        NULLIF(publication_explanation, ''),
        'Published automatically because the associated course occurrence period is completed. Pending information is shown as - until it is recorded.'
    )
WHERE gs.state = 'draft'
  AND (
        (
            EXISTS (
                SELECT 1
                FROM associate_grade_sheet_class_group agscg
                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
            )
            AND NOT EXISTS (
                SELECT 1
                FROM associate_grade_sheet_class_group agscg
                JOIN class_group cg
                  ON cg.id_class_group = agscg.id_class_group
                JOIN course_occurrence_period cop
                  ON cop.id_course_occurrence_period = cg.id_course_occurrence_period
                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
                  AND (cop.state = 'cancelled' OR cop.ends_at >= CURRENT_DATE)
            )
        )
        OR (
            NOT EXISTS (
                SELECT 1
                FROM associate_grade_sheet_class_group agscg
                WHERE agscg.id_grade_sheet = gs.id_grade_sheet
            )
            AND EXISTS (
                SELECT 1
                FROM course_occurrence co
                JOIN integrate_subject isub
                  ON isub.id_course = co.id_course
                 AND isub.id_subject = gs.id_subject
                JOIN course_occurrence_period cop
                  ON cop.id_course_occurrence = gs.id_course_occurrence
                 AND cop.curricular_year = isub.curricular_year
                 AND cop.term = isub.term
                WHERE co.id_course_occurrence = gs.id_course_occurrence
                  AND cop.state <> 'cancelled'
                  AND cop.ends_at < CURRENT_DATE
            )
        )
    );
