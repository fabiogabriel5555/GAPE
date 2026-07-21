UPDATE grade_sheet
SET title = CONCAT('Grade sheet - ', SUBSTRING(title, CHAR_LENGTH('Pauta - ') + 1))
WHERE title LIKE 'Pauta - %';
