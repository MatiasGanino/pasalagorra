-- Usuarios de prueba para desarrollo y tests (admin/admin, user/user).
-- Esta carpeta solo la cargan los perfiles dev y test: en prod no existen.

INSERT INTO jhi_user (id, login, password_hash, first_name, last_name, email, activated, lang_key, created_by, created_date)
VALUES (1, 'admin', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC',
        'Administrator', 'Administrator', 'admin@localhost', TRUE, 'es', 'system', NOW()),
       (2, 'user', '$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K',
        'User', 'User', 'user@localhost', TRUE, 'es', 'system', NOW());

INSERT INTO jhi_user_authority (user_id, authority_name)
VALUES (1, 'ROLE_ADMIN'), (1, 'ROLE_USER'), (2, 'ROLE_USER');
