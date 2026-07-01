-- Add access metadata for the lesson leaderboard route introduced in Course Service.
-- Existing databases need this migration because editing techhub.sql only affects new databases.

WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('COURSE_LESSON_LEADERBOARD_READ', 'Get lesson leaderboard', '/api/courses/{courseId}/lessons/{lessonId}/leaderboard', 'GET'::permission_method, 'COURSES')
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

INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
JOIN permissions p ON p.name = 'COURSE_LESSON_LEADERBOARD_READ' AND p.is_active = 'Y'
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'INSTRUCTOR', 'LEARNER')
  AND r.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

WITH seed(url_pattern, method, security_level, description) AS (
    VALUES
        ('/api/courses/{courseId}/lessons/{lessonId}/leaderboard', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated lesson leaderboard')
), updated AS (
    UPDATE endpoint_security_policies p
    SET security_level = seed.security_level,
        description = seed.description,
        is_active = 'Y',
        updated = CURRENT_TIMESTAMP
    FROM seed
    WHERE p.url_pattern = seed.url_pattern
      AND p.method = seed.method
    RETURNING p.url_pattern
)
INSERT INTO endpoint_security_policies (url_pattern, method, security_level, description, is_active, created, updated)
SELECT seed.url_pattern, seed.method, seed.security_level, seed.description, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed
WHERE NOT EXISTS (
    SELECT 1
    FROM endpoint_security_policies p
    WHERE p.url_pattern = seed.url_pattern
      AND p.method = seed.method
);
