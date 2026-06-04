-- Testes de violacao de PK
-- Cada statement deve falhar.

-- PK simples duplicada
INSERT INTO organization (id_organization, name, acronym, type, state)
VALUES (10, 'Org duplicada PK', 'ODP', 'company', 'active');

-- PK composta duplicada
INSERT INTO grant_administrator (id_admin_user, cod_permission)
VALUES (1, 'MANAGE_USERS');

-- Privacy nao pode repetir cod_privacy no mesmo utilizador
INSERT INTO user_privacy (id_user, cod_privacy, value_flag, updated_at)
VALUES
    (1, 'profile_visibility', 1, '2026-03-01 09:00:00'),
    (1, 'profile_visibility', 0, '2026-03-01 09:01:00');
