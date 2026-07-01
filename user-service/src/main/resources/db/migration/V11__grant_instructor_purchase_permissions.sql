-- Instructors are also learners: they must be able to purchase/enroll in courses.
-- The INSTRUCTOR role was missing the payment-create permissions that LEARNER has,
-- causing 403 Forbidden on POST /api/payments/paypal/create (and VNPay).
-- Grant the proxy payment-create permissions to INSTRUCTOR.

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT roles.id, permissions.id, NOW()
FROM roles
JOIN permissions ON permissions.name IN (
    'PAYMENT_PAYPAL_CREATE_PROXY',
    'PAYMENT_VNPAY_CREATE_PROXY'
)
WHERE roles.name = 'INSTRUCTOR'
ON CONFLICT (role_id, permission_id) DO UPDATE SET granted_at = EXCLUDED.granted_at;
