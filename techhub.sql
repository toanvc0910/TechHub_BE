-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Define ENUM types
CREATE TYPE user_status AS ENUM('ACTIVE', 'INACTIVE', 'BANNED');
CREATE TYPE lang AS ENUM('VI', 'EN', 'JA');
CREATE TYPE auth_provider AS ENUM('GOOGLE', 'FACEBOOK', 'GITHUB');
CREATE TYPE otp_type AS ENUM('REGISTER', 'LOGIN', 'RESET');
CREATE TYPE course_status AS ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED');
CREATE TYPE course_level AS ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL_LEVELS');
CREATE TYPE content_type AS ENUM('VIDEO', 'TEXT', 'EXERCISE');
CREATE TYPE exercise_type AS ENUM('MULTIPLE_CHOICE', 'CODING', 'OPEN_ENDED');
CREATE TYPE enrollment_status AS ENUM('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED');
CREATE TYPE rating_target AS ENUM('COURSE', 'LESSON');
CREATE TYPE comment_target AS ENUM('COURSE', 'LESSON', 'BLOG', 'VIDEO', 'CODE');
CREATE TYPE transaction_status AS ENUM('PENDING', 'COMPLETED', 'REFUNDED');
CREATE TYPE payment_method AS ENUM('MOMO', 'ZALOPAY', 'CREDIT_CARD', 'BANK_TRANSFER');
CREATE TYPE payment_status AS ENUM('SUCCESS', 'FAILED');
CREATE TYPE blog_status AS ENUM('DRAFT', 'PUBLISHED');
CREATE TYPE leaderboard_type AS ENUM('GLOBAL', 'COURSE', 'PATH');
CREATE TYPE notification_type AS ENUM('PROGRESS', 'NEW_COURSE', 'COMMENT', 'ACCOUNT', 'BLOG', 'SYSTEM');
CREATE TYPE delivery_method AS ENUM('EMAIL', 'PUSH', 'IN_APP');
CREATE TYPE event_type AS ENUM('VIEW', 'COMPLETE', 'EXERCISE');
CREATE TYPE translation_target AS ENUM('COURSE', 'LESSON', 'BLOG', 'EXERCISE', 'CHAPTER');
CREATE TYPE chat_sender AS ENUM('USER', 'BOT');
CREATE TYPE lesson_asset_type AS ENUM('VIDEO', 'DOCUMENT', 'EXTERNAL_LINK', 'CODE_TEMPLATE', 'SUPPLEMENT');
CREATE TYPE test_case_visibility AS ENUM('PUBLIC', 'PRIVATE');
CREATE TYPE submission_status AS ENUM('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'PARTIAL', 'ERROR');
-- Add missing permission_method ENUM type
CREATE TYPE permission_method AS ENUM('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS');
CREATE TYPE skill_category AS ENUM('LANGUAGE', 'FRAMEWORK', 'TOOL', 'CONCEPT', 'OTHER');
-- AI Service ENUM types (Not used - columns use VARCHAR instead)
-- CREATE TYPE ai_task_type AS ENUM('EXERCISE_GENERATION', 'LEARNING_PATH', 'RECOMMENDATION_REALTIME', 'RECOMMENDATION_SCHEDULED', 'CHAT_GENERAL', 'CHAT_ADVISOR');
-- CREATE TYPE ai_task_status AS ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'DRAFT');

-- Users Table
-- Note: Roles are now managed through roles and user_roles tables only
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255),
    password_hash VARCHAR(255),
    avatar VARCHAR(500),
    status user_status DEFAULT 'ACTIVE',
    login_type VARCHAR(50) DEFAULT 'LOCAL',
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_login_type ON users(login_type);
CREATE INDEX idx_users_created ON users(created);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_by ON users(created_by);

-- Profiles Table
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    avatar_url TEXT,
    bio TEXT,
    location VARCHAR(255),
    preferred_language lang DEFAULT 'VI',
    learning_history JSONB DEFAULT '{}'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_is_active ON profiles(is_active);

-- Authentication Logs
CREATE TABLE authentication_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    login_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    device VARCHAR(255),
    success BOOLEAN NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_auth_logs_user_id ON authentication_logs(user_id);
CREATE INDEX idx_auth_logs_login_time ON authentication_logs(login_time);
CREATE INDEX idx_auth_logs_is_active ON authentication_logs(is_active);

-- Auth Providers
CREATE TABLE auth_providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider auth_provider NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_auth_providers_user_provider ON auth_providers(user_id, provider);
CREATE INDEX idx_auth_providers_user_id ON auth_providers(user_id);
CREATE INDEX idx_auth_providers_is_active ON auth_providers(is_active);

-- OTPs
CREATE TABLE otps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(6) NOT NULL,
    type otp_type NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_otps_user_id ON otps(user_id);
CREATE INDEX idx_otps_expires_at ON otps(expires_at);
CREATE INDEX idx_otps_is_active ON otps(is_active);

-- User TwoFA
CREATE TABLE user_twofa (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    secret VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT FALSE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_user_twofa_enabled ON user_twofa(enabled);
CREATE INDEX idx_user_twofa_is_active ON user_twofa(is_active);

-- Permissions Table (Corrected)
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), -- Changed from user_status to UUID
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id), -- Changed from user_status to UUID
    description VARCHAR(500),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    method permission_method NOT NULL, -- Using the newly defined ENUM
    name VARCHAR(255) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id), -- Changed from user_status to UUID
    url VARCHAR(500) NOT NULL
);
CREATE INDEX idx_permissions_method ON permissions(method);
CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_permissions_is_active ON permissions(is_active);

-- Roles Table (Added)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_is_active ON roles(is_active);

-- Role Permissions Join Table (Added)
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    PRIMARY KEY (role_id, permission_id)
);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_role_permissions_is_active ON role_permissions(is_active);

-- User Roles Join Table (Added and Corrected)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, -- Changed from user_status to UUID
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_user_roles_is_active ON user_roles(is_active);

-- User Permissions Join Table (per-user overrides on top of role-based permissions)
CREATE TABLE user_permissions (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    allowed BOOLEAN NOT NULL DEFAULT TRUE, -- TRUE = explicit allow; FALSE = explicit deny
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    PRIMARY KEY (user_id, permission_id)
);
CREATE INDEX idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX idx_user_permissions_permission_id ON user_permissions(permission_id);
CREATE INDEX idx_user_permissions_is_active ON user_permissions(is_active);

-- Courses Table
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    instructor_id UUID NOT NULL REFERENCES users(id),
    status course_status DEFAULT 'DRAFT',
    level course_level DEFAULT 'ALL_LEVELS',
    language lang DEFAULT 'VI',
    discount_price DECIMAL(10,2),
    promo_end_date TIMESTAMP WITH TIME ZONE,
    thumbnail VARCHAR(500),
    intro_video_file VARCHAR(500),
    objectives JSONB DEFAULT '[]'::JSONB,
    requirements JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_courses_instructor_id ON courses(instructor_id);
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_level ON courses(level);
CREATE INDEX idx_courses_language ON courses(language);
CREATE INDEX idx_courses_title_trgm ON courses USING GIN (title gin_trgm_ops);
CREATE INDEX idx_courses_is_active ON courses(is_active);
CREATE INDEX idx_courses_created ON courses(created);
CREATE INDEX idx_courses_objectives_gin ON courses USING GIN (objectives);
CREATE INDEX idx_courses_requirements_gin ON courses USING GIN (requirements);

-- Chapters Table
CREATE TABLE chapters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    "order" INTEGER NOT NULL,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    min_completion_threshold FLOAT DEFAULT 0.7 CHECK (min_completion_threshold BETWEEN 0 AND 1),
    auto_unlock BOOLEAN DEFAULT TRUE,
    locked BOOLEAN DEFAULT TRUE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_chapters_course_id ON chapters(course_id);
CREATE UNIQUE INDEX uniq_chapters_order_per_course ON chapters(course_id, "order");
CREATE INDEX idx_chapters_is_active ON chapters(is_active);

-- Lessons Table
CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    "order" INTEGER NOT NULL,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    content_type content_type NOT NULL,
    content TEXT,
    mandatory BOOLEAN DEFAULT TRUE,
    completion_weight FLOAT DEFAULT 1 CHECK (completion_weight >= 0),
    estimated_duration INTEGER,
    is_free BOOLEAN DEFAULT FALSE,
    workspace_enabled BOOLEAN DEFAULT FALSE,
    workspace_languages TEXT[] DEFAULT '{}'::TEXT[],
    workspace_template JSONB DEFAULT '{}'::JSONB,
    video_url TEXT,
    document_urls JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_lessons_chapter_id ON lessons(chapter_id);
CREATE UNIQUE INDEX uniq_lessons_order_per_chapter ON lessons(chapter_id, "order");
CREATE INDEX idx_lessons_is_active ON lessons(is_active);
CREATE INDEX idx_lessons_is_free ON lessons(is_free);
CREATE INDEX idx_lessons_content_type ON lessons(content_type);

-- Lesson Assets Table
CREATE TABLE lesson_assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    asset_type lesson_asset_type NOT NULL,
    "order" INTEGER NOT NULL DEFAULT 0,
    title VARCHAR(255),
    description TEXT,
    file_id UUID,
    external_url TEXT,
    metadata JSONB DEFAULT '{}'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    CHECK (file_id IS NOT NULL OR external_url IS NOT NULL OR asset_type = 'CODE_TEMPLATE')
);
CREATE INDEX idx_lesson_assets_lesson_id ON lesson_assets(lesson_id);
CREATE INDEX idx_lesson_assets_order ON lesson_assets(lesson_id, "order");
CREATE INDEX idx_lesson_assets_file_id ON lesson_assets(file_id);
CREATE INDEX idx_lesson_assets_is_active ON lesson_assets(is_active);

-- Exercises Table
CREATE TABLE exercises (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type exercise_type NOT NULL,
    question TEXT NOT NULL,
    test_cases JSONB,
    lesson_id UUID UNIQUE NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    options JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_exercises_lesson_id ON exercises(lesson_id);
CREATE INDEX idx_exercises_type ON exercises(type);
CREATE INDEX idx_exercises_test_cases_gin ON exercises USING GIN (test_cases);
CREATE INDEX idx_exercises_is_active ON exercises(is_active);

-- Exercise Test Cases Table
CREATE TABLE exercise_test_cases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    "order" INTEGER NOT NULL DEFAULT 0,
    visibility test_case_visibility DEFAULT 'PUBLIC',
    input TEXT,
    expected_output TEXT,
    weight FLOAT DEFAULT 1 CHECK (weight >= 0),
    timeout_seconds INTEGER,
    sample BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_exercise_test_cases_exercise_id ON exercise_test_cases(exercise_id);
CREATE INDEX idx_exercise_test_cases_order ON exercise_test_cases(exercise_id, "order");
CREATE INDEX idx_exercise_test_cases_visibility ON exercise_test_cases(visibility);
CREATE INDEX idx_exercise_test_cases_is_active ON exercise_test_cases(is_active);

-- Progress Table
CREATE TABLE progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    completion FLOAT DEFAULT 0.0,
    completed_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_progress_user_lesson ON progress(user_id, lesson_id);
CREATE INDEX idx_progress_user_id ON progress(user_id);
CREATE INDEX idx_progress_completion ON progress(completion);
CREATE INDEX idx_progress_is_active ON progress(is_active);

-- Comments Table (Polymorphic)
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    parent_id UUID REFERENCES comments(id),
    target_id UUID NOT NULL,
    target_type comment_target NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_target_id_type ON comments(target_id, target_type);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
CREATE INDEX idx_comments_is_active ON comments(is_active);

-- Enrollments
CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status enrollment_status DEFAULT 'ENROLLED',
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_enrollments_user_course ON enrollments(user_id, course_id);
CREATE INDEX idx_enrollments_user_id ON enrollments(user_id);
CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
CREATE INDEX idx_enrollments_is_active ON enrollments(is_active);

-- Ratings
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id UUID NOT NULL,
    target_type rating_target NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_ratings_user_target ON ratings(user_id, target_id, target_type);
CREATE INDEX idx_ratings_target_id_type ON ratings(target_id, target_type);
CREATE INDEX idx_ratings_score ON ratings(score);
CREATE INDEX idx_ratings_is_active ON ratings(is_active);

-- Submissions
CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    answer TEXT,
    submission_data JSONB,
    grade FLOAT,
    graded_at TIMESTAMP WITH TIME ZONE,
    graded_by UUID REFERENCES users(id),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status submission_status NOT NULL DEFAULT 'PENDING',
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_submissions_user_id ON submissions(user_id);
CREATE INDEX idx_submissions_exercise_id ON submissions(exercise_id);
CREATE INDEX idx_submissions_grade ON submissions(grade);
CREATE INDEX idx_submissions_status ON submissions(status);
CREATE INDEX idx_submissions_is_active ON submissions(is_active);

-- User Codes
CREATE TABLE user_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    saved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_user_codes_user_lesson ON user_codes(user_id, lesson_id);
CREATE INDEX idx_user_codes_user_id ON user_codes(user_id);
CREATE INDEX idx_user_codes_lesson_id ON user_codes(lesson_id);
CREATE INDEX idx_user_codes_is_active ON user_codes(is_active);

-- Promotions
CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percent INTEGER NOT NULL CHECK (discount_percent BETWEEN 0 AND 100),
    applicable_courses JSONB DEFAULT '[]'::JSONB,
    expires_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_promotions_code ON promotions(code);
CREATE INDEX idx_promotions_expires_at ON promotions(expires_at);
CREATE INDEX idx_promotions_is_active ON promotions(is_active);

-- Transactions Table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(10,2) NOT NULL,
    status transaction_status NOT NULL,
    refund_reason TEXT,
    refund_amount DECIMAL(10,2),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created ON transactions(created);
CREATE INDEX idx_transactions_is_active ON transactions(is_active);

-- Transaction Items
CREATE TABLE transaction_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    price_at_purchase DECIMAL(10,2) NOT NULL,
    quantity INTEGER DEFAULT 1,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_transaction_items_transaction_id ON transaction_items(transaction_id);
CREATE INDEX idx_transaction_items_course_id ON transaction_items(course_id);
CREATE INDEX idx_transaction_items_is_active ON transaction_items(is_active);

-- Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    method payment_method NOT NULL,
    status payment_status NOT NULL,
    gateway_response JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_method ON payments(method);
CREATE INDEX idx_payments_is_active ON payments(is_active);

-- Carts Table
CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    items JSONB NOT NULL DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_items_gin ON carts USING GIN (items);
CREATE INDEX idx_carts_is_active ON carts(is_active);

-- Blogs Table
CREATE TABLE blogs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    thumbnail VARCHAR(500),
    author_id UUID NOT NULL REFERENCES users(id),
    status blog_status DEFAULT 'DRAFT',
    attachments JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_blogs_author_id ON blogs(author_id);
CREATE INDEX idx_blogs_status ON blogs(status);
CREATE INDEX idx_blogs_title_trgm ON blogs USING GIN (title gin_trgm_ops);
CREATE INDEX idx_blogs_is_active ON blogs(is_active);

-- Forums
CREATE TABLE forums (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    course_id UUID REFERENCES courses(id) ON DELETE SET NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_forums_course_id ON forums(course_id);
CREATE INDEX idx_forums_created_by ON forums(created_by);
CREATE INDEX idx_forums_is_active ON forums(is_active);

-- Forum Posts
CREATE TABLE forum_posts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    forum_id UUID NOT NULL REFERENCES forums(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    parent_id UUID REFERENCES forum_posts(id),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_forum_posts_forum_id ON forum_posts(forum_id);
CREATE INDEX idx_forum_posts_user_id ON forum_posts(user_id);
CREATE INDEX idx_forum_posts_parent_id ON forum_posts(parent_id);
CREATE INDEX idx_forum_posts_is_active ON forum_posts(is_active);

-- Group Chats
CREATE TABLE group_chats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id UUID REFERENCES courses(id) ON DELETE SET NULL,
    title VARCHAR(255),
    participants JSONB DEFAULT '[]'::JSONB,
    messages JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_group_chats_course_id ON group_chats(course_id);
CREATE INDEX idx_group_chats_participants_gin ON group_chats USING GIN (participants);
CREATE INDEX idx_group_chats_is_active ON group_chats(is_active);

-- Learning Paths Table
CREATE TABLE learning_paths (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    layout_edges JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_learning_paths_title ON learning_paths(title);
CREATE INDEX idx_learning_paths_layout_edges_gin ON learning_paths USING GIN (layout_edges);
CREATE INDEX idx_learning_paths_is_active ON learning_paths(is_active);

-- Learning Path Courses Join Table
CREATE TABLE learning_path_courses (
    path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    "order" INTEGER NOT NULL,
    position_x INTEGER,
    position_y INTEGER,
    is_optional VARCHAR(1) DEFAULT 'N' CHECK (is_optional IN ('Y', 'N')),
    PRIMARY KEY (path_id, course_id)
);
CREATE INDEX idx_path_courses_path_id ON learning_path_courses(path_id);
CREATE INDEX idx_path_courses_position ON learning_path_courses(position_x, position_y);

-- Skills Table (Kỹ năng cho courses và learning paths)
CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    thumbnail VARCHAR(500),
    category skill_category DEFAULT 'OTHER',
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_skills_is_active ON skills(is_active);

-- Tags Table (Thẻ tag cho courses và blogs)
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_tags_is_active ON tags(is_active);

-- Course Skills Join Table
CREATE TABLE course_skills (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, skill_id)
);

CREATE INDEX idx_course_skills_course_id ON course_skills(course_id);
CREATE INDEX idx_course_skills_skill_id ON course_skills(skill_id);

-- Course Tags Join Table
CREATE TABLE course_tags (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, tag_id)
);

CREATE INDEX idx_course_tags_course_id ON course_tags(course_id);
CREATE INDEX idx_course_tags_tag_id ON course_tags(tag_id);

-- Learning Path Skills Join Table
CREATE TABLE learning_path_skills (
    path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (path_id, skill_id)
);

CREATE INDEX idx_learning_path_skills_path_id ON learning_path_skills(path_id);
CREATE INDEX idx_learning_path_skills_skill_id ON learning_path_skills(skill_id);

-- Blog Tags Join Table
CREATE TABLE blog_tags (
    blog_id UUID NOT NULL REFERENCES blogs(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (blog_id, tag_id)
);

CREATE INDEX idx_blog_tags_blog_id ON blog_tags(blog_id);
CREATE INDEX idx_blog_tags_tag_id ON blog_tags(tag_id);

-- Path Progress Table
CREATE TABLE path_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    path_id UUID NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    completion FLOAT DEFAULT 0.0,
    milestones JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_path_progress_user_path ON path_progress(user_id, path_id);
CREATE INDEX idx_path_progress_user_id ON path_progress(user_id);
CREATE INDEX idx_path_progress_is_active ON path_progress(is_active);

-- Badges Table
CREATE TABLE badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    criteria JSONB NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_badges_name ON badges(name);
CREATE INDEX idx_badges_is_active ON badges(is_active);

-- User Badges Join Table
CREATE TABLE user_badges (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    awarded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, badge_id)
);
CREATE INDEX idx_user_badges_user_id ON user_badges(user_id);

-- User Points
CREATE TABLE user_points (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    points INTEGER DEFAULT 0,
    history JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_user_points_points ON user_points(points);
CREATE INDEX idx_user_points_is_active ON user_points(is_active);

-- Leaderboards
CREATE TABLE leaderboards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type leaderboard_type NOT NULL,
    course_id UUID REFERENCES courses(id),
    path_id UUID REFERENCES learning_paths(id),
    scores JSONB DEFAULT '[]'::JSONB,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_leaderboards_type ON leaderboards(type);
CREATE INDEX idx_leaderboards_course_id ON leaderboards(course_id);
CREATE INDEX idx_leaderboards_path_id ON leaderboards(path_id);
CREATE INDEX idx_leaderboards_is_active ON leaderboards(is_active);

-- Rewards
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    points_required INTEGER NOT NULL,
    discount_code VARCHAR(50),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_rewards_points_required ON rewards(points_required);
CREATE INDEX idx_rewards_is_active ON rewards(is_active);

-- Notifications Table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type notification_type NOT NULL,
    title VARCHAR(255),
    message TEXT NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    delivery_method delivery_method NOT NULL DEFAULT 'IN_APP',
    metadata JSONB,
    sent_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_created ON notifications(created);
CREATE INDEX idx_notifications_is_active ON notifications(is_active);

-- Analytics Table (Partitioned)
CREATE TABLE analytics (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    course_id UUID REFERENCES courses(id),
    study_time BIGINT,
    score FLOAT,
    event_type event_type NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    device VARCHAR(255),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    PRIMARY KEY (id, timestamp)
) PARTITION BY RANGE (timestamp);
CREATE INDEX idx_analytics_user_id ON analytics(user_id);
CREATE INDEX idx_analytics_course_id ON analytics(course_id);
CREATE INDEX idx_analytics_timestamp ON analytics(timestamp);
CREATE INDEX idx_analytics_event_type ON analytics(event_type);
CREATE INDEX idx_analytics_is_active ON analytics(is_active);

-- Recommendations
CREATE TABLE recommendations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recommended_courses JSONB DEFAULT '[]'::JSONB,
    recommended_paths JSONB DEFAULT '[]'::JSONB,
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);
CREATE INDEX idx_recommendations_generated_at ON recommendations(generated_at);
CREATE INDEX idx_recommendations_is_active ON recommendations(is_active);

-- Translations
CREATE TABLE translations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    target_id UUID NOT NULL,
    target_type translation_target NOT NULL,
    language lang NOT NULL,
    translated_title VARCHAR(255),
    translated_content TEXT,
    translated_description TEXT,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE UNIQUE INDEX uniq_translations_target_lang ON translations(target_id, target_type, language);
CREATE INDEX idx_translations_target_id_type ON translations(target_id, target_type);
CREATE INDEX idx_translations_language ON translations(language);
CREATE INDEX idx_translations_is_active ON translations(is_active);

-- Chat Sessions Table
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    context JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_started_at ON chat_sessions(started_at);
CREATE INDEX idx_chat_sessions_is_active ON chat_sessions(is_active);

-- Chat Messages Table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender chat_sender NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_timestamp ON chat_messages(timestamp);
CREATE INDEX idx_chat_messages_is_active ON chat_messages(is_active);

-- Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    details JSONB,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_is_active ON audit_logs(is_active);

-- Trigger for updated
CREATE OR REPLACE FUNCTION update_updated()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_active = 'Y' THEN
        NEW.updated = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t text;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'users', 'profiles', 'authentication_logs', 'auth_providers', 'otps', 'user_twofa',
        'permissions', 'roles', 'role_permissions', 'user_roles', 'courses', 'chapters',
        'lessons', 'exercises', 'progress', 'comments', 'enrollments', 'ratings',
        'submissions', 'user_codes', 'promotions', 'transactions', 'transaction_items',
        'payments', 'carts', 'blogs', 'forums', 'forum_posts', 'group_chats',
        'learning_paths', 'path_progress', 'badges', 'user_points', 'leaderboards',
        'rewards', 'notifications', 'analytics', 'recommendations', 'translations',
        'chat_sessions', 'chat_messages', 'audit_logs', 'ai_generation_tasks',
        'skills', 'tags'
    ]
    LOOP
        EXECUTE 'CREATE TRIGGER trg_update_' || t || ' BEFORE UPDATE ON ' || t || ' FOR EACH ROW EXECUTE PROCEDURE update_updated();';
    END LOOP;
END;
$$;

-- ===========================
-- FILE MANAGEMENT SYSTEM
-- ===========================

-- Add ENUM for file types
CREATE TYPE file_type_enum AS ENUM('IMAGE', 'VIDEO', 'DOCUMENT', 'AUDIO', 'OTHER');

-- Folders Table (Thư mục)
CREATE TABLE file_folders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES file_folders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    path TEXT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

CREATE INDEX idx_file_folders_user_id ON file_folders(user_id);
CREATE INDEX idx_file_folders_parent_id ON file_folders(parent_id);
CREATE INDEX idx_file_folders_path ON file_folders(path);
CREATE INDEX idx_file_folders_is_active ON file_folders(is_active);
CREATE UNIQUE INDEX uniq_file_folders_user_name_parent ON file_folders(user_id, name, COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid));

-- Files Table (File metadata)
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    folder_id UUID REFERENCES file_folders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    
    cloudinary_public_id VARCHAR(500) NOT NULL UNIQUE,
    cloudinary_url TEXT NOT NULL,
    cloudinary_secure_url TEXT NOT NULL,
    
    file_type file_type_enum NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    duration INTEGER,
    
    alt_text VARCHAR(500),
    caption TEXT,
    description TEXT,
    tags TEXT[] DEFAULT '{}',
    
    upload_source VARCHAR(50) DEFAULT 'DIRECT',
    reference_id UUID,
    reference_type VARCHAR(50),
    
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_files_folder_id ON files(folder_id);
CREATE INDEX idx_files_file_type ON files(file_type);
CREATE INDEX idx_files_cloudinary_public_id ON files(cloudinary_public_id);
CREATE INDEX idx_files_tags_gin ON files USING GIN (tags);
CREATE INDEX idx_files_created ON files(created);
CREATE INDEX idx_files_is_active ON files(is_active);
CREATE INDEX idx_files_reference ON files(reference_id, reference_type);
CREATE INDEX idx_files_name_trgm ON files USING GIN (name gin_trgm_ops);

-- Backfill foreign key constraint now that files table exists
ALTER TABLE lesson_assets
    ADD CONSTRAINT fk_lesson_assets_file
    FOREIGN KEY (file_id)
    REFERENCES files(id)
    ON DELETE SET NULL;

-- File Usage Tracking
CREATE TABLE file_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    used_in_type VARCHAR(50) NOT NULL,
    used_in_id UUID NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_file_usage_file_id ON file_usage(file_id);
CREATE INDEX idx_file_usage_used_in ON file_usage(used_in_type, used_in_id);

-- Triggers for file tables

-- ===========================
-- AI SERVICE TABLES
-- ===========================

-- AI Generation Tasks Table
-- Note: Using VARCHAR instead of ENUM types for Hibernate compatibility
-- Valid task_type values: 'EXERCISE_GENERATION', 'LEARNING_PATH_GENERATION', 'RECOMMENDATION_REALTIME', 'RECOMMENDATION_SCHEDULED', 'CHAT_GENERAL', 'CHAT_ADVISOR'
-- Valid status values: 'DRAFT', 'APPROVED', 'REJECTED', 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'
-- target_reference: For EXERCISE_GENERATION = lesson_id, for LEARNING_PATH_GENERATION = user_id or empty
CREATE TABLE ai_generation_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    target_reference VARCHAR(255),
    model_used VARCHAR(128),
    prompt TEXT,
    request_payload JSONB,
    result_payload JSONB,
    error_message TEXT,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

CREATE INDEX idx_ai_generation_tasks_task_type ON ai_generation_tasks(task_type);
CREATE INDEX idx_ai_generation_tasks_status ON ai_generation_tasks(status);
CREATE INDEX idx_ai_generation_tasks_target_reference ON ai_generation_tasks(target_reference);
CREATE INDEX idx_ai_generation_tasks_created ON ai_generation_tasks(created);
CREATE INDEX idx_ai_generation_tasks_is_active ON ai_generation_tasks(is_active);
CREATE INDEX idx_ai_generation_tasks_target_status ON ai_generation_tasks(target_reference, status, task_type);
CREATE TRIGGER trg_update_file_folders 
BEFORE UPDATE ON file_folders 
FOR EACH ROW EXECUTE PROCEDURE update_updated();

CREATE TRIGGER trg_update_files 
BEFORE UPDATE ON files 
FOR EACH ROW EXECUTE PROCEDURE update_updated();

-- Function to get folder full path
CREATE OR REPLACE FUNCTION get_folder_path(folder_id UUID) 
RETURNS TEXT AS $$
DECLARE
    path_result TEXT;
BEGIN
    WITH RECURSIVE folder_tree AS (
        SELECT id, name, parent_id, name as path
        FROM file_folders
        WHERE id = folder_id
        
        UNION ALL
        
        SELECT f.id, f.name, f.parent_id, f.name || '/' || ft.path
        FROM file_folders f
        INNER JOIN folder_tree ft ON f.id = ft.parent_id
    )
    SELECT '/' || path INTO path_result
    FROM folder_tree
    WHERE parent_id IS NULL;
    
    RETURN COALESCE(path_result, '/');
END;
$$ LANGUAGE plpgsql;

-- Function to update folder path
CREATE OR REPLACE FUNCTION update_folder_path() 
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_id IS NULL THEN
        NEW.path = '/' || NEW.name;
    ELSE
        SELECT path || '/' || NEW.name INTO NEW.path
        FROM file_folders
        WHERE id = NEW.parent_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_folder_path
BEFORE INSERT OR UPDATE ON file_folders
FOR EACH ROW
EXECUTE PROCEDURE update_folder_path();
