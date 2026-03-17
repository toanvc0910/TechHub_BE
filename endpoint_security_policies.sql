-- Endpoint Security Policies migration (for existing DB / upgrade path)
-- Security levels:
--   PUBLIC        = no JWT, no permission check
--   AUTHENTICATED = JWT required, no permission check
--   AUTHORIZED    = JWT required + RBAC permission check (default)

-- 1. Ensure enum type exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'security_level') THEN
        CREATE TYPE security_level AS ENUM ('PUBLIC', 'AUTHENTICATED', 'AUTHORIZED');
    END IF;
END$$;

-- 2. Ensure table exists
CREATE TABLE IF NOT EXISTS endpoint_security_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    url_pattern VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL DEFAULT '*',
    security_level security_level NOT NULL,
    description VARCHAR(500),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_esp_security_level ON endpoint_security_policies(security_level);
CREATE INDEX IF NOT EXISTS idx_esp_is_active ON endpoint_security_policies(is_active);

-- 3. Seed data — PUBLIC endpoints (no JWT, no permission)
INSERT INTO endpoint_security_policies (url_pattern, method, security_level, description)
SELECT *
FROM (
    VALUES
        ('/api/auth/**',                '*', 'PUBLIC'::security_level, 'Auth endpoints (login, register, verify, etc.)'),
        ('/api/users',                  'POST', 'PUBLIC'::security_level, 'User registration'),
        ('/api/users/forgot-password',  '*', 'PUBLIC'::security_level, 'Forgot password'),
        ('/api/users/reset-password/**','*', 'PUBLIC'::security_level, 'Reset password'),
        ('/api/users/resend-reset-code/**', '*', 'PUBLIC'::security_level, 'Resend reset code'),
        ('/api/users/public/**',        '*', 'PUBLIC'::security_level, 'Public user endpoints (instructors, etc.)'),
        ('/actuator/**',                '*', 'PUBLIC'::security_level, 'Spring Actuator health/info'),
        ('/swagger-ui/**',              '*', 'PUBLIC'::security_level, 'Swagger UI'),
        ('/v3/api-docs/**',             '*', 'PUBLIC'::security_level, 'OpenAPI docs'),
        ('/oauth2/**',                  '*', 'PUBLIC'::security_level, 'OAuth2 flow'),
        ('/api/files/**',               '*', 'PUBLIC'::security_level, 'File access'),
        ('/api/folders/**',             '*', 'PUBLIC'::security_level, 'Folder access'),
        ('/api/file-usage/**',          '*', 'PUBLIC'::security_level, 'File usage stats'),
        ('/api/ai/chat/stream/**',      '*', 'PUBLIC'::security_level, 'AI Chat SSE streaming'),
        ('/api/payment/**',             '*', 'PUBLIC'::security_level, 'Payment callbacks and public operations'),
        ('/api/payments/**',            '*', 'PUBLIC'::security_level, 'Payment queries'),
        ('/api/transactions/**',        '*', 'PUBLIC'::security_level, 'Transaction queries')
) AS v(url_pattern, method, security_level, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM endpoint_security_policies p
    WHERE p.url_pattern = v.url_pattern
      AND p.method = v.method
      AND p.security_level = v.security_level
      AND p.is_active = 'Y'
);

-- 4. Seed data — AUTHENTICATED endpoints (JWT required, no RBAC)
INSERT INTO endpoint_security_policies (url_pattern, method, security_level, description)
SELECT *
FROM (
    VALUES
        ('/api/users/profile',          '*', 'AUTHENTICATED'::security_level, 'Current user profile'),
        ('/api/users/change-password',  'POST', 'AUTHENTICATED'::security_level, 'Change own password'),
        ('/api/users/{userId}',         'GET', 'AUTHENTICATED'::security_level, 'View user by ID')
) AS v(url_pattern, method, security_level, description)
WHERE NOT EXISTS (
    SELECT 1
    FROM endpoint_security_policies p
    WHERE p.url_pattern = v.url_pattern
      AND p.method = v.method
      AND p.security_level = v.security_level
      AND p.is_active = 'Y'
);

-- Note: any endpoint NOT listed here defaults to AUTHORIZED (JWT + RBAC permission check)
