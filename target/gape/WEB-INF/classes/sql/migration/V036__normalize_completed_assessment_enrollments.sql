-- Completed assessments cannot retain actionable enrollment requests.  Keep
-- the audit history, but make the terminal state explicit and close its date.
UPDATE enroll_assessment enrollment
JOIN assessment assessment_row
  ON assessment_row.id_assessment = enrollment.id_assessment
SET enrollment.state = 'completed',
    enrollment.end_date = CASE
        WHEN assessment_row.available_until IS NOT NULL
             AND (enrollment.end_date IS NULL
                  OR enrollment.end_date > DATE(assessment_row.available_until))
            THEN DATE(assessment_row.available_until)
        ELSE enrollment.end_date
    END
WHERE enrollment.state = 'active'
  AND assessment_row.state = 'completed';

UPDATE enroll_assessment enrollment
JOIN assessment assessment_row
  ON assessment_row.id_assessment = enrollment.id_assessment
SET enrollment.state = 'rejected',
    enrollment.end_date = CASE
        WHEN assessment_row.available_until IS NOT NULL
             AND (enrollment.end_date IS NULL
                  OR enrollment.end_date > DATE(assessment_row.available_until))
            THEN DATE(assessment_row.available_until)
        ELSE enrollment.end_date
    END
WHERE enrollment.state = 'pending'
  AND assessment_row.state = 'completed';
