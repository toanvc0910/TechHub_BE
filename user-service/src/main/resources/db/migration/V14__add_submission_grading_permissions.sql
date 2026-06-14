-- Grading & submission-review permissions for instructors/admins.
-- The proxy exposes two new endpoints (list submissions per exercise, grade a
-- submission). Their paths have an extra path segment compared with the existing
-- exercise permissions, so AntPathMatcher does NOT cover them — they need their
-- own permission rows or the gateway denies with 403.

WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('COURSE_SUBMISSIONS_READ', 'List learner submissions for an exercise',
            '/api/courses/{courseId}/lessons/{lessonId}/exercises/{exerciseId}/submissions',
            'GET'::permission_method, 'COURSES'),
        ('COURSE_SUBMISSION_GRADE', 'Grade a learner submission',
            '/api/courses/{courseId}/lessons/{lessonId}/submissions/{submissionId}/grade',
            'PUT'::permission_method, 'COURSES')
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

-- Grant to SUPER_ADMIN and ADMIN (full access).
WITH admin_roles AS (
    SELECT id FROM roles WHERE name IN ('SUPER_ADMIN', 'ADMIN') AND is_active = 'Y'
), seeded_permissions AS (
    SELECT id FROM permissions
    WHERE name IN ('COURSE_SUBMISSIONS_READ', 'COURSE_SUBMISSION_GRADE') AND is_active = 'Y'
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM admin_roles r
CROSS JOIN seeded_permissions p
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

-- Grant to INSTRUCTOR (instructors grade learners in their own courses).
WITH baseline(role_name, permission_name) AS (
    VALUES
        ('INSTRUCTOR', 'COURSE_SUBMISSIONS_READ'),
        ('INSTRUCTOR', 'COURSE_SUBMISSION_GRADE')
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM baseline b
JOIN roles r ON r.name = b.role_name AND r.is_active = 'Y'
JOIN permissions p ON p.name = b.permission_name AND p.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;
