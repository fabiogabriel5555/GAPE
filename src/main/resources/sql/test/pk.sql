-- Testes de violacao de PK
-- Cada statement deve falhar.

-- PK simples duplicada
INSERT INTO organization (id_organization, name, acronym, type, state)
VALUES (10, 'Org duplicada PK', 'ODP', 'company', 'active');

-- PK composta duplicada
INSERT INTO grant_administrator (id_admin_user, cod_permission, context_type, context_id)
VALUES (1, 'MANAGE_ALL', 'GLOBAL', 0);

-- Um estudante nao pode repetir a mesma inscricao no mesmo curso
INSERT INTO enroll_course (id_student_user, id_course, state, start_date, end_date)
VALUES
    (4, 30, 'active', '2026-03-01', NULL),
    (4, 30, 'active', '2026-03-02', NULL);
