CREATE TABLE IF NOT EXISTS learning_streaks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_streak INTEGER NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak INTEGER NOT NULL DEFAULT 0 CHECK (longest_streak >= 0),
    last_activity_date DATE,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_learning_streaks_user_id
    ON learning_streaks(user_id)
    WHERE is_active = 'Y';

CREATE INDEX IF NOT EXISTS idx_learning_streaks_last_activity_date
    ON learning_streaks(last_activity_date);

CREATE INDEX IF NOT EXISTS idx_learning_streaks_is_active
    ON learning_streaks(is_active);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_learning_streaks_updated'
    ) THEN
        CREATE TRIGGER trg_learning_streaks_updated
        BEFORE UPDATE ON learning_streaks
        FOR EACH ROW
        EXECUTE FUNCTION update_updated();
    END IF;
END $$;

WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('COURSE_STREAK_READ', 'Get learning streak', '/api/courses/streak', 'GET'::permission_method, 'COURSES')
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
JOIN permissions p ON p.name = 'COURSE_STREAK_READ' AND p.is_active = 'Y'
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'INSTRUCTOR', 'LEARNER')
  AND r.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

WITH seed(url_pattern, method, security_level, description) AS (
    VALUES
        ('/api/courses/streak', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated learning streak')
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
