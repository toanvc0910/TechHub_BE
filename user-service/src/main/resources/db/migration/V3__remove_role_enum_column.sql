-- Migration to remove role enum column from users table
-- Now users will get roles only from user_roles table

-- Drop the role column if it exists
ALTER TABLE users DROP COLUMN IF EXISTS role;

-- Drop the user_role enum type if it exists
DROP TYPE IF EXISTS user_role CASCADE;

-- Add comment to document the change
COMMENT ON TABLE users IS 'Users table - roles are now managed through user_roles table only';
COMMENT ON TABLE user_roles IS 'User roles relationship - primary source of user role information';
