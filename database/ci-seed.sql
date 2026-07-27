-- Minimal fixture data for CI / fresh local databases.
--
-- schema.sql seeds categories and menu items but no users or employees, so anything that needs
-- a valid userId (the /api/checkout endpoint, the Cucumber checkout scenarios, and the live
-- ApiIntegrationTest) has nothing to reference on a brand-new database. This adds exactly one
-- employee + one user (both id = 1) so those tests have a valid foreign key to use.
--
-- The password_hash/salt values below are NOT real credentials and cannot be used to log in
-- through LoginFrame — this user only exists to satisfy the users.id foreign key on orders.
--
-- Usage:
--   mysql -uroot -p pos_system < database/schema.sql
--   mysql -uroot -p pos_system < database/ci-seed.sql
--
-- (The GitHub Actions CI workflow — .github/workflows/ci.yml — runs both automatically.)

USE pos_system;

INSERT INTO employees (id, full_name, phone, email, position, hourly_rate, active)
VALUES (1, 'CI Test Employee', '555-0100', 'ci-employee@test.local', 'Cashier', 15.00, TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO users (id, username, password_hash, salt, full_name, role, employee_id, active)
VALUES (1, 'ci-admin', 'not-a-real-hash-ci-only', 'not-a-real-salt-ci-only', 'CI Admin', 'ADMIN', 1, TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);
