WITH inserted_permission AS (
    INSERT INTO permissions (name, description, url, method, resource, is_active, created, updated)
    VALUES (
        'PAYOUT_SUMMARY_READ',
        'Read payout operations summary',
        '/api/payments/payouts/summary',
        'GET'::permission_method,
        'PAYOUTS',
        TRUE,
        NOW(),
        NOW()
    )
    ON CONFLICT (name) DO UPDATE SET
        description = EXCLUDED.description,
        url = EXCLUDED.url,
        method = EXCLUDED.method,
        resource = EXCLUDED.resource,
        is_active = TRUE,
        updated = NOW()
    RETURNING id
)
INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT roles.id, permissions.id, NOW()
FROM roles
JOIN permissions ON permissions.name = 'PAYOUT_SUMMARY_READ'
WHERE roles.name IN ('ADMIN', 'SUPER_ADMIN', 'INSTRUCTOR')
ON CONFLICT (role_id, permission_id) DO UPDATE SET granted_at = EXCLUDED.granted_at;
