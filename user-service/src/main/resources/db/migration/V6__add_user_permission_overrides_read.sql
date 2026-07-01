-- Add the DB permission required by the account-level permission override UI.
-- This is intentionally DB-seeded so adding the API does not require backend hard-coding.

WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('USER_PERMISSION_OVERRIDES_READ', 'Read active user permission overrides', '/api/users/{userId}/permissions/overrides', 'GET'::permission_method, 'PERMISSIONS')
), updated AS (
    UPDATE permissions p
    SET description = seed.description,
        url = seed.url,
        method = seed.method,
        resource = seed.resource,
        is_active = 'Y',
        updated = CURRENT_TIMESTAMP
    FROM seed
    WHERE p.name = seed.name
    RETURNING p.name
)
INSERT INTO permissions (name, description, url, method, resource, is_active, created, updated)
SELECT seed.name, seed.description, seed.url, seed.method, seed.resource, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = seed.name);

WITH admin_roles AS (
    SELECT id
    FROM roles
    WHERE name IN ('SUPER_ADMIN', 'ADMIN')
      AND is_active = 'Y'
), seeded_permissions AS (
    SELECT id
    FROM permissions
    WHERE name = 'USER_PERMISSION_OVERRIDES_READ'
      AND is_active = 'Y'
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM admin_roles r
CROSS JOIN seeded_permissions p
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;
