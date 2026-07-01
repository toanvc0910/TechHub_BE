WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('AI_REINDEX_DATA_CONTRACT', 'Reindex AI data contract', '/api/ai/admin/reindex-data-contract', 'POST'::permission_method, 'AI'),
        ('AI_DATA_CONTRACT_READ', 'Read AI data contract', '/api/ai/admin/data-contract', 'GET'::permission_method, 'AI'),
        ('AI_DATA_CONTRACT_VALIDATE', 'Validate AI data contract against database schema', '/api/ai/admin/data-contract/validate', 'GET'::permission_method, 'AI'),
        ('AI_DATA_CONTRACT_SYNC', 'Sync AI data contract registry', '/api/ai/admin/data-contract/sync', 'POST'::permission_method, 'AI')
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
    WHERE name IN (
        'AI_REINDEX_DATA_CONTRACT',
        'AI_DATA_CONTRACT_READ',
        'AI_DATA_CONTRACT_VALIDATE',
        'AI_DATA_CONTRACT_SYNC'
    )
      AND is_active = 'Y'
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM admin_roles r
CROSS JOIN seeded_permissions p
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;
