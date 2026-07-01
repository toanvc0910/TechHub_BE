-- Fix role assignments for bootstrap users created by the old DatabaseInitializer.
-- This does not touch passwords or profile data.
-- No temp tables are used so the script is safe in SQL clients with autocommit enabled.

WITH seed(name, description) AS (
    VALUES
        ('SUPER_ADMIN', 'Super administrator'),
        ('ADMIN', 'Administrator'),
        ('INSTRUCTOR', 'Instructor'),
        ('LEARNER', 'Learner')
), updated AS (
    UPDATE roles r
    SET description = seed.description,
        is_active = 'Y',
        updated = CURRENT_TIMESTAMP
    FROM seed
    WHERE r.name = seed.name
    RETURNING r.name
)
INSERT INTO roles (name, description, is_active, created, updated)
SELECT seed.name, seed.description, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = seed.name);

-- Keep ADMIN and SUPER_ADMIN fully provisioned when these roles are newly added.
WITH canonical_roles AS (
    SELECT DISTINCT ON (name)
        id,
        name
    FROM roles
    WHERE name IN ('SUPER_ADMIN', 'ADMIN', 'INSTRUCTOR', 'LEARNER')
    ORDER BY name, (is_active = 'Y') DESC, created ASC, id::text ASC
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM canonical_roles r
CROSS JOIN permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND p.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

-- Disable wrong or duplicate active role assignments only for the three legacy bootstrap users.
WITH expected_roles(email, role_name) AS (
    VALUES
        ('admin@techhub.com', 'SUPER_ADMIN'),
        ('admin@techhub.com', 'ADMIN'),
        ('instructor@techhub.com', 'INSTRUCTOR'),
        ('learner@techhub.com', 'LEARNER')
), canonical_roles AS (
    SELECT DISTINCT ON (name)
        id,
        name
    FROM roles
    WHERE name IN ('SUPER_ADMIN', 'ADMIN', 'INSTRUCTOR', 'LEARNER')
    ORDER BY name, (is_active = 'Y') DESC, created ASC, id::text ASC
)
UPDATE user_roles ur
SET is_active = 'N',
    updated = CURRENT_TIMESTAMP
FROM users u, roles r
WHERE ur.user_id = u.id
  AND ur.role_id = r.id
  AND u.email IN ('admin@techhub.com', 'instructor@techhub.com', 'learner@techhub.com')
  AND ur.is_active = 'Y'
  AND (
      NOT EXISTS (
          SELECT 1
          FROM expected_roles expected
          WHERE expected.email = u.email
            AND expected.role_name = r.name
      )
      OR NOT EXISTS (
          SELECT 1
          FROM canonical_roles canonical
          WHERE canonical.id = ur.role_id
            AND canonical.name = r.name
      )
  );

-- Add the expected canonical role assignments.
WITH expected_roles(email, role_name) AS (
    VALUES
        ('admin@techhub.com', 'SUPER_ADMIN'),
        ('admin@techhub.com', 'ADMIN'),
        ('instructor@techhub.com', 'INSTRUCTOR'),
        ('learner@techhub.com', 'LEARNER')
), canonical_roles AS (
    SELECT DISTINCT ON (name)
        id,
        name
    FROM roles
    WHERE name IN ('SUPER_ADMIN', 'ADMIN', 'INSTRUCTOR', 'LEARNER')
    ORDER BY name, (is_active = 'Y') DESC, created ASC, id::text ASC
)
INSERT INTO user_roles (user_id, role_id, is_active, assigned_at, created, updated)
SELECT u.id, r.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM expected_roles expected
JOIN users u ON u.email = expected.email
JOIN canonical_roles r ON r.name = expected.role_name
ON CONFLICT (user_id, role_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;
