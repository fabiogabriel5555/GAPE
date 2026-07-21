-- Subject grade sheets are now derived exclusively from the final grade sheets
-- of their class groups in the same course occurrence. Remove the old direct
-- assessment configuration and invalidate values that were calculated from it.
DELETE boa
FROM based_on_assessment boa
JOIN grade_sheet gs ON gs.id_grade_sheet = boa.id_grade_sheet
LEFT JOIN associate_grade_sheet_class_group agscg
  ON agscg.id_grade_sheet = gs.id_grade_sheet
WHERE agscg.id_grade_sheet IS NULL;

UPDATE grade_record gr
JOIN grade_sheet gs ON gs.id_grade_sheet = gr.id_grade_sheet
LEFT JOIN associate_grade_sheet_class_group agscg
  ON agscg.id_grade_sheet = gs.id_grade_sheet
SET gr.state = 'inactive',
    gr.notes = 'Marked inactive because subject grade sheets are consolidated from published class group grade sheets.'
WHERE agscg.id_grade_sheet IS NULL
  AND gr.state IN ('draft', 'published', 'corrected');

UPDATE grade_sheet gs
LEFT JOIN associate_grade_sheet_class_group agscg
  ON agscg.id_grade_sheet = gs.id_grade_sheet
SET gs.state = 'draft',
    gs.released_at = NULL,
    gs.weight_alert = 'This subject grade sheet is automatically consolidated from the published class group grade sheets.'
WHERE agscg.id_grade_sheet IS NULL
  AND gs.state NOT IN ('closed', 'inactive');

UPDATE certificate c
JOIN based_on_grade_sheet_certificate bgsc
  ON bgsc.id_certificate = c.id_certificate
JOIN grade_sheet gs ON gs.id_grade_sheet = bgsc.id_grade_sheet
LEFT JOIN associate_grade_sheet_class_group agscg
  ON agscg.id_grade_sheet = gs.id_grade_sheet
SET c.state = 'draft',
    c.validation_code = NULL,
    c.issued_at = NULL,
    c.final_grade = NULL
WHERE agscg.id_grade_sheet IS NULL;
