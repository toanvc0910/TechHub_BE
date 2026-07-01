-- Fix AI permission gaps found by comparing proxy-client source with DB permission seed.
-- Existing DBs need this because editing techhub.sql only affects new local databases.

WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('AI_RECOMMEND_HISTORY', 'Read recommendation history', '/api/ai/recommendations/history', 'GET'::permission_method, 'AI'),
        ('AI_DRAFTS_EXERCISES_BATCH', 'Get exercise drafts in batch', '/api/ai/drafts/exercises/batch', 'POST'::permission_method, 'AI'),
        ('AI_PROVIDER_CONFIG_UPDATE', 'Update AI provider config', '/api/ai/admin/provider-config', 'POST'::permission_method, 'AI'),
        ('AI_INGEST_FILE_UPLOADED', 'Ingest uploaded file into AI index', '/api/ai/admin/ingest-file-uploaded', 'POST'::permission_method, 'AI')
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
        'AI_RECOMMEND_HISTORY',
        'AI_DRAFTS_EXERCISES_BATCH',
        'AI_PROVIDER_CONFIG_UPDATE',
        'AI_INGEST_FILE_UPLOADED'
    )
      AND is_active = 'Y'
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM admin_roles r
CROSS JOIN seeded_permissions p
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

WITH baseline(role_name, permission_name) AS (
    VALUES
        ('INSTRUCTOR', 'AI_RECOMMEND_HISTORY'),
        ('INSTRUCTOR', 'AI_DRAFTS_EXERCISES_BATCH'),
        ('LEARNER', 'AI_RECOMMEND_HISTORY')
), canonical_roles AS (
    SELECT id, name
    FROM roles
    WHERE name IN ('INSTRUCTOR', 'LEARNER')
      AND is_active = 'Y'
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM baseline b
JOIN canonical_roles r ON r.name = b.role_name
JOIN permissions p ON p.name = b.permission_name
WHERE p.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;
