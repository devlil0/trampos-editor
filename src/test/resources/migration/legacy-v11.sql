-- Dados sintéticos do esquema V11, antes de existir users.username.
INSERT INTO users (id, email, password_hash, display_name, active, created_at) VALUES
('10000000-0000-0000-0000-000000000001', 'cliente@example.test', 'hash-de-fixture', 'Marina Luz', TRUE, '2020-01-01 10:00:00'),
('20000000-0000-0000-0000-000000000002', 'editor@example.test', NULL, 'Marina Luz', TRUE, '2020-01-02 10:00:00'),
('30000000-0000-0000-0000-000000000003', 'antiga@example.test', 'hash-de-fixture', 'Ana Café', TRUE, '2020-01-03 10:00:00'),
('40000000-0000-0000-0000-000000000004', 'recente@example.test', 'hash-de-fixture', 'Ana Café', FALSE, '2020-01-04 10:00:00'),
('50000000-0000-0000-0000-000000000005', 'simbolos@example.test', 'hash-de-fixture', '!!!', TRUE, '2020-01-05 10:00:00');

INSERT INTO user_roles (user_id, role) VALUES
('10000000-0000-0000-0000-000000000001', 'CLIENT'),
('20000000-0000-0000-0000-000000000002', 'EDITOR');

INSERT INTO editor_profiles (id, user_id, slug, vertical, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
 'marina-luz', 'YOUTUBE', '2020-01-02 10:00:00');

INSERT INTO user_identities (id, user_id, provider, subject, created_at) VALUES
('b0000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000002',
 'github', 'fixture-editor', '2020-01-02 10:00:00');
