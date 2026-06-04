-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- Define ENUM types
CREATE TYPE user_status AS ENUM('ACTIVE', 'INACTIVE', 'BANNED');
CREATE TYPE lang AS ENUM('VI', 'EN', 'JA');
CREATE TYPE auth_provider AS ENUM('LOCAL', 'GOOGLE', 'FACEBOOK', 'GITHUB');
CREATE TYPE otp_type AS ENUM('REGISTER', 'LOGIN', 'RESET');
CREATE TYPE course_status AS ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED');
CREATE TYPE course_level AS ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL_LEVELS');
CREATE TYPE content_type AS ENUM('VIDEO', 'TEXT', 'EXERCISE');
CREATE TYPE exercise_type AS ENUM('MULTIPLE_CHOICE', 'CODING', 'OPEN_ENDED');
CREATE TYPE enrollment_status AS ENUM('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED');
CREATE TYPE rating_target AS ENUM('COURSE', 'LESSON');
CREATE TYPE comment_target AS ENUM('COURSE', 'LESSON', 'BLOG', 'VIDEO', 'CODE');
CREATE TYPE transaction_status AS ENUM('PENDING', 'COMPLETED', 'REFUNDED');
CREATE TYPE payment_method AS ENUM('MOMO', 'ZALOPAY', 'VNPAY', 'PAYPAL', 'CREDIT_CARD', 'BANK_TRANSFER');
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
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
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
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL DEFAULT 1,
    options JSONB DEFAULT '[]'::JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);
CREATE INDEX idx_exercises_lesson_id ON exercises(lesson_id);
CREATE INDEX idx_exercises_lesson_order ON exercises(lesson_id, order_index) WHERE is_active = 'Y';
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
-- Learning Streaks Table
CREATE TABLE learning_streaks (
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
CREATE UNIQUE INDEX uniq_learning_streaks_user_id ON learning_streaks(user_id) WHERE is_active = 'Y';
CREATE INDEX idx_learning_streaks_last_activity_date ON learning_streaks(last_activity_date);
CREATE INDEX idx_learning_streaks_is_active ON learning_streaks(is_active);
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
    original_amount DECIMAL(14,2),
    original_currency VARCHAR(3),
    gateway_amount DECIMAL(14,2),
    gateway_currency VARCHAR(3),
    fx_rate DECIMAL(18,8),
    fx_provider VARCHAR(64),
    fx_quoted_at TIMESTAMP WITH TIME ZONE,
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
    price_currency VARCHAR(3) NOT NULL DEFAULT 'VND',
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
-- Payment Gateway Mappings Table
CREATE TABLE payment_gateway_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    gateway_order_id VARCHAR(255) NOT NULL UNIQUE,
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    gateway_type VARCHAR(50) NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payment_gateway_mappings_gateway_order_id ON payment_gateway_mappings(gateway_order_id);
CREATE INDEX idx_payment_gateway_mappings_transaction_id ON payment_gateway_mappings(transaction_id);
CREATE INDEX idx_payment_gateway_mappings_gateway_type ON payment_gateway_mappings(gateway_type);
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
    tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    related_course_ids UUID[] NOT NULL DEFAULT ARRAY[]::UUID[],
    related_lesson_ids UUID[] NOT NULL DEFAULT ARRAY[]::UUID[],
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
CREATE INDEX idx_blogs_tags_gin ON blogs USING GIN (tags);
CREATE INDEX idx_blogs_related_course_ids ON blogs USING GIN (related_course_ids);
CREATE INDEX idx_blogs_related_lesson_ids ON blogs USING GIN (related_lesson_ids);
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
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (course_id, skill_id)  -- Prevent duplicate course-skill pairs
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
    metadata JSONB,
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
        'lessons', 'exercises', 'progress', 'learning_streaks', 'comments', 'enrollments', 'ratings',
        'submissions', 'user_codes', 'promotions', 'transactions', 'transaction_items',
        'payments', 'carts', 'blogs', 'forums', 'forum_posts', 'group_chats',
        'learning_paths', 'path_progress', 'badges', 'user_points', 'leaderboards',
        'rewards', 'notifications', 'analytics', 'recommendations', 'translations',
        'chat_sessions', 'chat_messages', 'audit_logs',
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
    storage_provider VARCHAR(50) NOT NULL DEFAULT 'MINIO',
    bucket_name VARCHAR(255),
    object_key VARCHAR(1000),
    public_url TEXT,
    secure_url TEXT,
    thumbnail_object_key VARCHAR(1000),
    thumbnail_url TEXT,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'READY' CHECK (processing_status IN ('PENDING', 'READY', 'FAILED')),
    processing_error TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
   
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
CREATE INDEX idx_files_processing_status ON files(processing_status);
CREATE INDEX idx_files_object_key ON files(object_key);
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
CREATE TRIGGER trg_update_ai_generation_tasks BEFORE UPDATE ON ai_generation_tasks FOR EACH ROW EXECUTE PROCEDURE update_updated();
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

-- Endpoint security baseline for proxy-client (DB-driven policies)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'security_level') THEN
        CREATE TYPE security_level AS ENUM ('PUBLIC', 'AUTHENTICATED', 'AUTHORIZED');
    END IF;
END$$;

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
CREATE INDEX IF NOT EXISTS idx_esp_pattern_method_active ON endpoint_security_policies(url_pattern, method, is_active);

-- Instructor Applications (CV scan + admin approval)
CREATE TABLE IF NOT EXISTS instructor_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cv_file_id UUID NOT NULL,
    cv_file_url TEXT,
    ai_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (ai_status IN ('PENDING','PROCESSED','FAILED')),
    ai_extracted_data JSONB,
    ai_error TEXT,
    admin_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (admin_status IN ('PENDING','APPROVED','REJECTED')),
    admin_note TEXT,
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y','N'))
);
CREATE INDEX IF NOT EXISTS idx_instructor_apps_user ON instructor_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_instructor_apps_admin_status ON instructor_applications(admin_status);
CREATE INDEX IF NOT EXISTS idx_instructor_apps_created ON instructor_applications(created);

-- CCCD front/back scan columns
ALTER TABLE instructor_applications
    ADD COLUMN IF NOT EXISTS cccd_front_file_id UUID,
    ADD COLUMN IF NOT EXISTS cccd_front_file_url TEXT,
    ADD COLUMN IF NOT EXISTS cccd_front_status VARCHAR(20) DEFAULT 'PENDING' CHECK (cccd_front_status IN ('PENDING','PROCESSED','FAILED')),
    ADD COLUMN IF NOT EXISTS cccd_front_data JSONB,
    ADD COLUMN IF NOT EXISTS cccd_front_error TEXT,
    ADD COLUMN IF NOT EXISTS cccd_back_file_id UUID,
    ADD COLUMN IF NOT EXISTS cccd_back_file_url TEXT,
    ADD COLUMN IF NOT EXISTS cccd_back_status VARCHAR(20) DEFAULT 'PENDING' CHECK (cccd_back_status IN ('PENDING','PROCESSED','FAILED')),
    ADD COLUMN IF NOT EXISTS cccd_back_data JSONB,
    ADD COLUMN IF NOT EXISTS cccd_back_error TEXT;

-- Certificates (1-N)
-- Drop nếu schema cũ sai kiểu cột (varchar thay vì UUID) do Hibernate ddl-auto.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'instructor_application_certificates'
          AND column_name = 'application_id'
          AND data_type <> 'uuid'
    ) THEN
        DROP TABLE instructor_application_certificates CASCADE;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS instructor_application_certificates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES instructor_applications(id) ON DELETE CASCADE,
    file_id UUID NOT NULL,
    file_url TEXT,
    ai_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (ai_status IN ('PENDING','PROCESSED','FAILED')),
    ai_data JSONB,
    ai_error TEXT,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y','N'))
);
CREATE INDEX IF NOT EXISTS idx_instructor_app_certs_app ON instructor_application_certificates(application_id);

-- Instructor profile: structured data trích từ AI khi đơn được duyệt.
-- 1-1 với users; cập nhật mỗi lần admin approve đơn mới (giữ snapshot mới nhất).
CREATE TABLE IF NOT EXISTS instructor_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    source_application_id UUID REFERENCES instructor_applications(id) ON DELETE SET NULL,

    -- Từ CCCD mặt trước
    id_number VARCHAR(20),
    full_name VARCHAR(200),
    date_of_birth VARCHAR(20),
    gender VARCHAR(10),
    nationality VARCHAR(100),
    place_of_origin TEXT,
    place_of_residence TEXT,

    -- Từ CCCD mặt sau
    cccd_issue_date VARCHAR(20),
    cccd_issue_place TEXT,
    cccd_mrz TEXT,
    identifying_features TEXT,

    -- Từ CV
    cv_summary TEXT,
    cv_email VARCHAR(200),
    cv_phone VARCHAR(50),
    cv_location VARCHAR(200),
    linkedin_url TEXT,
    github_url TEXT,
    portfolio_url TEXT,
    years_of_experience INT,
    skills JSONB,
    languages JSONB,
    education JSONB,
    experience JSONB,
    projects JSONB,
    cv_certifications JSONB,

    -- Từ chứng chỉ uploaded (mảng các object đã trích xuất)
    certificates JSONB,

    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_instructor_profiles_app ON instructor_profiles(source_application_id);
CREATE INDEX IF NOT EXISTS idx_instructor_profiles_id_number ON instructor_profiles(id_number);

-- DB-first RBAC seed. New API/page permissions should be added here or by the admin permissions API, not in backend startup code.
WITH seed(name, description) AS (
    VALUES
        ('SUPER_ADMIN', 'Super administrator'),
        ('ADMIN', 'Administrator'),
        ('INSTRUCTOR', 'Instructor'),
        ('LEARNER', 'Learner')
), updated AS (
    UPDATE roles r
    SET description = seed.description,
        is_active = 'Y',
        updated = CURRENT_TIMESTAMP
    FROM seed
    WHERE r.name = seed.name
    RETURNING r.name
)
INSERT INTO roles (name, description, is_active, created, updated)
SELECT seed.name, seed.description, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM seed
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = seed.name);

WITH seed(name, description, url, method, resource) AS (
    VALUES
        ('USER_CREATE', 'Create user', '/api/users', 'POST'::permission_method, 'USERS'),
        ('USER_READ_ALL', 'Get all users', '/api/users', 'GET'::permission_method, 'USERS'),
        ('USER_READ', 'Get user by ID', '/api/users/{id}', 'GET'::permission_method, 'USERS'),
        ('USER_READ_EMAIL', 'Get user by email', '/api/users/email/{email}', 'GET'::permission_method, 'USERS'),
        ('USER_READ_USERNAME', 'Get user by username', '/api/users/username/{username}', 'GET'::permission_method, 'USERS'),
        ('USER_UPDATE', 'Update user', '/api/users/{id}', 'PUT'::permission_method, 'USERS'),
        ('USER_DELETE', 'Delete user', '/api/users/{id}', 'DELETE'::permission_method, 'USERS'),
        ('USER_PROFILE', 'Get user profile', '/api/users/profile', 'GET'::permission_method, 'USERS'),
        ('USER_ACTIVATE', 'Activate user', '/api/users/{id}/activate', 'POST'::permission_method, 'USERS'),
        ('USER_DEACTIVATE', 'Deactivate user', '/api/users/{id}/deactivate', 'POST'::permission_method, 'USERS'),
        ('USER_STATUS_CHANGE', 'Change user status', '/api/users/{id}/status/{status}', 'PUT'::permission_method, 'USERS'),
        ('USER_CHANGE_PASSWORD', 'Change password', '/api/users/change-password', 'POST'::permission_method, 'USERS'),
        ('ADMIN_PERMISSION_LIST', 'List all permissions', '/api/admin/permissions', 'GET'::permission_method, 'ADMIN'),
        ('ADMIN_PERMISSION_READ', 'Get permission by ID', '/api/admin/permissions/{id}', 'GET'::permission_method, 'ADMIN'),
        ('ADMIN_PERMISSION_CREATE', 'Create permission', '/api/admin/permissions', 'POST'::permission_method, 'ADMIN'),
        ('ADMIN_PERMISSION_UPDATE', 'Update permission', '/api/admin/permissions/{id}', 'PUT'::permission_method, 'ADMIN'),
        ('ADMIN_PERMISSION_DELETE', 'Delete permission', '/api/admin/permissions/{id}', 'DELETE'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_LIST', 'List all roles', '/api/admin/roles', 'GET'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_READ', 'Get role by ID', '/api/admin/roles/{id}', 'GET'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_CREATE', 'Create role', '/api/admin/roles', 'POST'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_UPDATE', 'Update role', '/api/admin/roles/{id}', 'PUT'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_DELETE', 'Delete role', '/api/admin/roles/{id}', 'DELETE'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_ASSIGN_PERMISSION', 'Assign permissions to role', '/api/admin/roles/{id}/permissions', 'POST'::permission_method, 'ADMIN'),
        ('ADMIN_ROLE_REMOVE_PERMISSION', 'Remove permission from role', '/api/admin/roles/{roleId}/permissions/{permissionId}', 'DELETE'::permission_method, 'ADMIN'),
        ('ADMIN_USER_ROLES', 'Get user roles', '/api/admin/users/{id}/roles', 'GET'::permission_method, 'ADMIN'),
        ('ADMIN_USER_ASSIGN_ROLE', 'Assign roles to user', '/api/admin/users/{id}/roles', 'POST'::permission_method, 'ADMIN'),
        ('ADMIN_USER_REMOVE_ROLE', 'Remove role from user', '/api/admin/users/{userId}/roles/{roleId}', 'DELETE'::permission_method, 'ADMIN'),
        ('BLOG_CREATE', 'Create blog', '/api/blogs', 'POST'::permission_method, 'BLOGS'),
        ('BLOG_READ_ALL', 'Read all blogs', '/api/blogs', 'GET'::permission_method, 'BLOGS'),
        ('BLOG_READ', 'Read blog by ID', '/api/blogs/{id}', 'GET'::permission_method, 'BLOGS'),
        ('BLOG_UPDATE', 'Update blog', '/api/blogs/{id}', 'PUT'::permission_method, 'BLOGS'),
        ('BLOG_DELETE', 'Delete blog', '/api/blogs/{id}', 'DELETE'::permission_method, 'BLOGS'),
        ('BLOG_TAGS', 'Get blog tags', '/api/blogs/tags', 'GET'::permission_method, 'BLOGS'),
        ('BLOG_COMMENT_READ', 'Get blog comments', '/api/blogs/{id}/comments', 'GET'::permission_method, 'BLOGS'),
        ('BLOG_COMMENT_CREATE', 'Add blog comment', '/api/blogs/{id}/comments', 'POST'::permission_method, 'BLOGS'),
        ('BLOG_COMMENT_DELETE', 'Delete blog comment', '/api/blogs/{blogId}/comments/{commentId}', 'DELETE'::permission_method, 'BLOGS'),
        ('COURSE_CREATE', 'Create course', '/api/courses', 'POST'::permission_method, 'COURSES'),
        ('COURSE_READ_ALL', 'Read all courses', '/api/courses', 'GET'::permission_method, 'COURSES'),
        ('COURSE_MY_COURSES', 'Read my courses', '/api/courses/my-courses', 'GET'::permission_method, 'COURSES'),
        ('COURSE_READ', 'Read course by ID', '/api/courses/{id}', 'GET'::permission_method, 'COURSES'),
        ('COURSE_UPDATE', 'Update course', '/api/courses/{id}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_DELETE', 'Delete course', '/api/courses/{id}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_ENROLL', 'Enroll in course', '/api/courses/{id}/enroll', 'POST'::permission_method, 'COURSES'),
        ('COURSE_CHAPTER_READ', 'Get course chapters', '/api/courses/{id}/chapters', 'GET'::permission_method, 'COURSES'),
        ('COURSE_CHAPTER_CREATE', 'Create chapter', '/api/courses/{id}/chapters', 'POST'::permission_method, 'COURSES'),
        ('COURSE_CHAPTER_UPDATE', 'Update chapter', '/api/courses/{courseId}/chapters/{chapterId}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_CHAPTER_DELETE', 'Delete chapter', '/api/courses/{courseId}/chapters/{chapterId}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_LESSON_READ', 'Get lesson detail', '/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/detail', 'GET'::permission_method, 'COURSES'),
        ('COURSE_LESSON_CREATE', 'Create lesson', '/api/courses/{courseId}/chapters/{chapterId}/lessons', 'POST'::permission_method, 'COURSES'),
        ('COURSE_LESSON_UPDATE', 'Update lesson', '/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_LESSON_DELETE', 'Delete lesson', '/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_LESSON_ASSET_CREATE', 'Create lesson asset', '/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets', 'POST'::permission_method, 'COURSES'),
        ('COURSE_LESSON_ASSET_UPDATE', 'Update lesson asset', '/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_LESSON_ASSET_DELETE', 'Delete lesson asset', '/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_PROGRESS_READ', 'Get course progress', '/api/courses/{id}/progress', 'GET'::permission_method, 'COURSES'),
        ('COURSE_STREAK_READ', 'Get learning streak', '/api/courses/streak', 'GET'::permission_method, 'COURSES'),
        ('COURSE_LESSON_PROGRESS_UPDATE', 'Update lesson progress', '/api/courses/{courseId}/lessons/{lessonId}/progress', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_LESSON_COMPLETE', 'Mark lesson complete', '/api/courses/{courseId}/lessons/{lessonId}/progress/complete', 'POST'::permission_method, 'COURSES'),
        ('COURSE_RATING_READ', 'Get course rating', '/api/courses/{id}/ratings', 'GET'::permission_method, 'COURSES'),
        ('COURSE_RATING_CREATE', 'Submit course rating', '/api/courses/{id}/ratings', 'POST'::permission_method, 'COURSES'),
        ('COURSE_COMMENT_READ', 'Get course comments', '/api/courses/{id}/comments', 'GET'::permission_method, 'COURSES'),
        ('COURSE_COMMENT_CREATE', 'Add course comment', '/api/courses/{id}/comments', 'POST'::permission_method, 'COURSES'),
        ('COURSE_LESSON_COMMENT_READ', 'Get lesson comments', '/api/courses/{courseId}/lessons/{lessonId}/comments', 'GET'::permission_method, 'COURSES'),
        ('COURSE_LESSON_COMMENT_CREATE', 'Add lesson comment', '/api/courses/{courseId}/lessons/{lessonId}/comments', 'POST'::permission_method, 'COURSES'),
        ('COURSE_WORKSPACE_COMMENT_READ', 'Get workspace comments', '/api/courses/{courseId}/lessons/{lessonId}/workspace/comments', 'GET'::permission_method, 'COURSES'),
        ('COURSE_WORKSPACE_COMMENT_CREATE', 'Add workspace comment', '/api/courses/{courseId}/lessons/{lessonId}/workspace/comments', 'POST'::permission_method, 'COURSES'),
        ('COURSE_COMMENT_DELETE', 'Delete comment', '/api/courses/{courseId}/comments/{commentId}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_EXERCISE_READ', 'Get exercise', '/api/courses/{courseId}/lessons/{lessonId}/exercise', 'GET'::permission_method, 'COURSES'),
        ('COURSE_EXERCISE_UPSERT', 'Create/Update exercise', '/api/courses/{courseId}/lessons/{lessonId}/exercise', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_EXERCISE_SUBMIT', 'Submit exercise', '/api/courses/{courseId}/lessons/{lessonId}/exercise/submissions', 'POST'::permission_method, 'COURSES'),
        ('COURSE_EXERCISES_READ', 'Get all exercises', '/api/courses/{courseId}/lessons/{lessonId}/exercises', 'GET'::permission_method, 'COURSES'),
        ('COURSE_LESSON_LEADERBOARD_READ', 'Get lesson leaderboard', '/api/courses/{courseId}/lessons/{lessonId}/leaderboard', 'GET'::permission_method, 'COURSES'),
        ('COURSE_EXERCISES_CREATE', 'Create exercises', '/api/courses/{courseId}/lessons/{lessonId}/exercises', 'POST'::permission_method, 'COURSES'),
        ('COURSE_EXERCISES_UPDATE', 'Update exercise', '/api/courses/{courseId}/lessons/{lessonId}/exercises/{exerciseId}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_EXERCISES_DELETE', 'Delete exercise', '/api/courses/{courseId}/lessons/{lessonId}/exercises/{exerciseId}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_WORKSPACE_READ', 'Get workspace', '/api/courses/{courseId}/lessons/{lessonId}/workspace', 'GET'::permission_method, 'COURSES'),
        ('COURSE_WORKSPACE_SAVE', 'Save workspace', '/api/courses/{courseId}/lessons/{lessonId}/workspace', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_SKILL_CREATE', 'Create skill', '/api/courses/skills', 'POST'::permission_method, 'COURSES'),
        ('COURSE_SKILL_READ', 'Get skill', '/api/courses/skills/{id}', 'GET'::permission_method, 'COURSES'),
        ('COURSE_SKILL_READ_ALL', 'Get all skills', '/api/courses/skills', 'GET'::permission_method, 'COURSES'),
        ('COURSE_SKILL_UPDATE', 'Update skill', '/api/courses/skills/{id}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_SKILL_DELETE', 'Delete skill', '/api/courses/skills/{id}', 'DELETE'::permission_method, 'COURSES'),
        ('COURSE_TAG_CREATE', 'Create tag', '/api/courses/tags', 'POST'::permission_method, 'COURSES'),
        ('COURSE_TAG_READ', 'Get tag', '/api/courses/tags/{id}', 'GET'::permission_method, 'COURSES'),
        ('COURSE_TAG_READ_ALL', 'Get all tags', '/api/courses/tags', 'GET'::permission_method, 'COURSES'),
        ('COURSE_TAG_UPDATE', 'Update tag', '/api/courses/tags/{id}', 'PUT'::permission_method, 'COURSES'),
        ('COURSE_TAG_DELETE', 'Delete tag', '/api/courses/tags/{id}', 'DELETE'::permission_method, 'COURSES'),
        ('ENROLLMENT_CREATE', 'Create enrollment', '/api/enrollments', 'POST'::permission_method, 'ENROLLMENTS'),
        ('ENROLLMENT_READ', 'Get enrollment by ID', '/api/enrollments/{enrollmentId}', 'GET'::permission_method, 'ENROLLMENTS'),
        ('ENROLLMENT_MY_ENROLLMENTS', 'Get my enrollments', '/api/enrollments/my-enrollments', 'GET'::permission_method, 'ENROLLMENTS'),
        ('LEARNING_PATH_CREATE', 'Create learning path', '/api/learning-paths', 'POST'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_READ_ALL', 'Read all learning paths', '/api/learning-paths', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_READ', 'Read learning path by ID', '/api/learning-paths/{id}', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_UPDATE', 'Update learning path', '/api/learning-paths/{id}', 'PUT'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_DELETE', 'Delete learning path', '/api/learning-paths/{id}', 'DELETE'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_SEARCH', 'Search learning paths', '/api/learning-paths/search', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_BY_CREATOR', 'Get learning paths by creator', '/api/learning-paths/creator/{userId}', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_BY_COURSE', 'Get learning paths by course', '/api/learning-paths/by-course/{courseId}', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_ADD_COURSES', 'Add courses to path', '/api/learning-paths/{id}/courses', 'POST'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_REMOVE_COURSE', 'Remove course from path', '/api/learning-paths/{pathId}/courses/{courseId}', 'DELETE'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_REORDER_COURSES', 'Reorder courses', '/api/learning-paths/{id}/courses/reorder', 'PUT'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_PROGRESS_UPSERT', 'Create/Update progress', '/api/learning-paths/progress', 'POST'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_PROGRESS_READ', 'Get progress by user and path', '/api/learning-paths/progress/user/{userId}/path/{pathId}', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_PROGRESS_BY_USER', 'Get progress by user', '/api/learning-paths/progress/user/{userId}', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_PROGRESS_BY_PATH', 'Get progress by path', '/api/learning-paths/progress/path/{pathId}', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_PROGRESS_DELETE', 'Delete progress', '/api/learning-paths/progress/user/{userId}/path/{pathId}', 'DELETE'::permission_method, 'LEARNING_PATHS'),
        ('LEARNING_PATH_STATISTICS', 'Get path statistics', '/api/learning-paths/{id}/statistics', 'GET'::permission_method, 'LEARNING_PATHS'),
        ('FILE_UPLOAD', 'Upload file', '/api/files/upload', 'POST'::permission_method, 'FILES'),
        ('FILE_UPLOAD_MULTIPLE', 'Upload multiple files', '/api/files/upload/multiple', 'POST'::permission_method, 'FILES'),
        ('FILE_READ', 'Get file', '/api/files/{id}', 'GET'::permission_method, 'FILES'),
        ('FILE_READ_ALL', 'List files', '/api/files', 'GET'::permission_method, 'FILES'),
        ('FILE_READ_BY_FOLDER', 'Get files by folder', '/api/files/folder/{id}', 'GET'::permission_method, 'FILES'),
        ('FILE_DELETE', 'Delete file', '/api/files/{id}', 'DELETE'::permission_method, 'FILES'),
        ('FILE_STATISTICS', 'Get file statistics', '/api/files/statistics', 'GET'::permission_method, 'FILES'),
        ('FOLDER_CREATE', 'Create folder', '/api/files/folders', 'POST'::permission_method, 'FILES'),
        ('FOLDER_READ_BY_USER', 'Get folders by user', '/api/files/folders/user/{id}', 'GET'::permission_method, 'FILES'),
        ('FOLDER_READ', 'Get folder', '/api/files/folders/{id}', 'GET'::permission_method, 'FILES'),
        ('FOLDER_READ_TREE', 'Get folder tree', '/api/files/folders/{id}/tree', 'GET'::permission_method, 'FILES'),
        ('FOLDER_UPDATE', 'Update folder', '/api/files/folders/{id}', 'PUT'::permission_method, 'FILES'),
        ('FOLDER_DELETE', 'Delete folder', '/api/files/folders/{id}', 'DELETE'::permission_method, 'FILES'),
        ('FILE_USAGE_TRACK', 'Track file usage', '/api/files/usage/track', 'POST'::permission_method, 'FILES'),
        ('FILE_USAGE_REMOVE', 'Remove file usage', '/api/files/usage/remove', 'DELETE'::permission_method, 'FILES'),
        ('FILE_USAGE_REMOVE_ALL', 'Remove all file usage', '/api/files/usage/remove-all', 'DELETE'::permission_method, 'FILES'),
        ('FILE_USAGE_READ', 'List file usages', '/api/files/usage/file/{id}', 'GET'::permission_method, 'FILES'),
        ('AI_GENERATE_EXERCISES', 'Generate exercises', '/api/ai/exercises/generate', 'POST'::permission_method, 'AI'),
        ('AI_GENERATE_LEARNING_PATH', 'Generate learning path', '/api/ai/learning-paths/generate', 'POST'::permission_method, 'AI'),
        ('AI_RECOMMEND_REALTIME', 'Recommend realtime', '/api/ai/recommendations/realtime', 'POST'::permission_method, 'AI'),
        ('AI_RECOMMEND_SCHEDULED', 'Recommend scheduled', '/api/ai/recommendations/scheduled', 'POST'::permission_method, 'AI'),
        ('AI_RECOMMEND_HISTORY', 'Read recommendation history', '/api/ai/recommendations/history', 'GET'::permission_method, 'AI'),
        ('AI_CHAT', 'AI Chat', '/api/ai/chat/messages', 'POST'::permission_method, 'AI'),
        ('AI_CHAT_STREAM', 'Stream AI chat', '/api/ai/chat/stream', 'POST'::permission_method, 'AI'),
        ('AI_CHAT_STREAM_SIMPLE', 'Stream simple AI chat', '/api/ai/chat/stream/simple', 'GET'::permission_method, 'AI'),
        ('AI_CHAT_SESSIONS', 'Get AI Chat Sessions', '/api/ai/chat/sessions', 'GET'::permission_method, 'AI'),
        ('AI_CHAT_SESSION_CREATE', 'Create AI Chat Session', '/api/ai/chat/sessions', 'POST'::permission_method, 'AI'),
        ('AI_CHAT_SESSION_DETAIL', 'Get AI Chat Session Detail', '/api/ai/chat/sessions/{sessionId}', 'GET'::permission_method, 'AI'),
        ('AI_CHAT_SESSION_DELETE', 'Delete AI Chat Session', '/api/ai/chat/sessions/{sessionId}', 'DELETE'::permission_method, 'AI'),
        ('AI_CHAT_SESSION_MESSAGES', 'Get AI Chat Session Messages', '/api/ai/chat/sessions/{sessionId}/messages', 'GET'::permission_method, 'AI'),
        ('AI_DRAFTS_EXERCISES', 'Get Exercise Drafts', '/api/ai/drafts/exercises', 'GET'::permission_method, 'AI'),
        ('AI_DRAFTS_EXERCISES_BATCH', 'Get exercise drafts in batch', '/api/ai/drafts/exercises/batch', 'POST'::permission_method, 'AI'),
        ('AI_DRAFTS_EXERCISES_LATEST', 'Get Latest Exercise Draft', '/api/ai/drafts/exercises/latest', 'GET'::permission_method, 'AI'),
        ('AI_DRAFTS_DETAIL', 'Get Draft by ID', '/api/ai/drafts/{taskId}', 'GET'::permission_method, 'AI'),
        ('AI_DRAFTS_LEARNING_PATHS', 'Get Learning Path Drafts', '/api/ai/drafts/learning-paths', 'GET'::permission_method, 'AI'),
        ('AI_DRAFTS_APPROVE_EXERCISE', 'Approve Exercise Draft', '/api/ai/drafts/{taskId}/approve-exercise', 'POST'::permission_method, 'AI'),
        ('AI_DRAFTS_APPROVE_LEARNING_PATH', 'Approve Learning Path Draft', '/api/ai/drafts/{taskId}/approve-learning-path', 'POST'::permission_method, 'AI'),
        ('AI_DRAFTS_REJECT', 'Reject Draft', '/api/ai/drafts/{taskId}/reject', 'POST'::permission_method, 'AI'),
        ('AI_REINDEX_COURSES', 'Reindex courses', '/api/ai/admin/reindex-courses', 'POST'::permission_method, 'AI'),
        ('AI_REINDEX_LESSONS', 'Reindex lessons', '/api/ai/admin/reindex-lessons', 'POST'::permission_method, 'AI'),
        ('AI_REINDEX_BLOGS', 'Reindex blogs', '/api/ai/admin/reindex-blogs', 'POST'::permission_method, 'AI'),
        ('AI_REINDEX_ALL', 'Reindex all', '/api/ai/admin/reindex-all', 'POST'::permission_method, 'AI'),
        ('AI_QDRANT_STATS', 'Qdrant stats', '/api/ai/admin/qdrant-stats', 'POST'::permission_method, 'AI'),
        ('NOTIFICATION_READ_ALL', 'Get all notifications', '/api/notifications', 'GET'::permission_method, 'NOTIFICATIONS'),
        ('NOTIFICATION_UNREAD_COUNT', 'Get unread notification count', '/api/notifications/count/unread', 'GET'::permission_method, 'NOTIFICATIONS'),
        ('NOTIFICATION_MARK_READ', 'Mark notification as read', '/api/notifications/{id}/read', 'PUT'::permission_method, 'NOTIFICATIONS'),
        ('NOTIFICATION_MARK_ALL_READ', 'Mark all notifications as read', '/api/notifications/read', 'PUT'::permission_method, 'NOTIFICATIONS'),
        ('PAYMENT_VNPAY_CREATE', 'Create VNPay payment', '/api/v1/payment/vn-pay', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_VNPAY_CALLBACK', 'VNPay payment callback', '/api/v1/payment/vn-pay-callback', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_PAYPAL_CREATE', 'Create PayPal payment', '/api/v1/payment/paypal/create', 'POST'::permission_method, 'PAYMENT'),
        ('PAYMENT_PAYPAL_SUCCESS', 'PayPal payment success', '/api/v1/payment/paypal/success', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_PAYPAL_CANCEL', 'PayPal payment cancel', '/api/v1/payment/paypal/cancel', 'GET'::permission_method, 'PAYMENT'),
        ('TRANSACTION_READ_BY_USER', 'Get user transactions', '/api/v1/transactions/user/{userId}', 'GET'::permission_method, 'PAYMENT'),
        ('TRANSACTION_READ', 'Get transaction by ID', '/api/v1/transactions/{transactionId}', 'GET'::permission_method, 'PAYMENT'),
        ('TRANSACTION_READ_BY_STATUS', 'Get transactions by status', '/api/v1/transactions/status/{status}', 'GET'::permission_method, 'PAYMENT'),
        ('TRANSACTION_PAYMENTS_READ', 'Get transaction payments', '/api/v1/transactions/{transactionId}/payments', 'GET'::permission_method, 'PAYMENT'),
        ('USER_EFFECTIVE_PERMISSIONS_READ', 'Read effective permissions for a user', '/api/users/{userId}/permissions/effective', 'GET'::permission_method, 'PERMISSIONS'),
        ('USER_PERMISSION_CATALOG_READ', 'Read paged user permission catalog', '/api/users/{userId}/permissions/catalog', 'GET'::permission_method, 'PERMISSIONS'),
        ('USER_PERMISSION_OVERRIDES_READ', 'Read active user permission overrides', '/api/users/{userId}/permissions/overrides', 'GET'::permission_method, 'PERMISSIONS'),
        ('USER_PERMISSION_CHECK', 'Check a user permission against an endpoint', '/api/users/{userId}/permissions/check', 'POST'::permission_method, 'PERMISSIONS'),
        ('USER_PERMISSION_OVERRIDE_UPSERT', 'Create or update a user permission override', '/api/users/{userId}/permissions', 'POST'::permission_method, 'PERMISSIONS'),
        ('USER_PERMISSION_OVERRIDE_REMOVE', 'Remove a user permission override', '/api/users/{userId}/permissions/{permissionId}', 'DELETE'::permission_method, 'PERMISSIONS'),
        ('ADMIN_ENDPOINT_POLICY_CREATE', 'Create endpoint security policy', '/api/admin/endpoint-security-policies', 'POST'::permission_method, 'ADMIN'),
        ('ADMIN_ENDPOINT_POLICY_UPDATE', 'Update endpoint security policy', '/api/admin/endpoint-security-policies/{id}', 'PUT'::permission_method, 'ADMIN'),
        ('ADMIN_ENDPOINT_POLICY_DELETE', 'Delete endpoint security policy', '/api/admin/endpoint-security-policies/{id}', 'DELETE'::permission_method, 'ADMIN'),
        ('INSTRUCTOR_APPLICATION_SUBMIT', 'Submit instructor application', '/api/users/instructor-applications', 'POST'::permission_method, 'INSTRUCTOR_APPLICATIONS'),
        ('INSTRUCTOR_APPLICATION_READ_OWN', 'Read own instructor application', '/api/users/instructor-applications/me', 'GET'::permission_method, 'INSTRUCTOR_APPLICATIONS'),
        ('INSTRUCTOR_APPLICATION_READ_ALL', 'List instructor applications', '/api/users/instructor-applications', 'GET'::permission_method, 'INSTRUCTOR_APPLICATIONS'),
        ('INSTRUCTOR_APPLICATION_READ', 'Read instructor application detail', '/api/users/instructor-applications/{id}', 'GET'::permission_method, 'INSTRUCTOR_APPLICATIONS'),
        ('INSTRUCTOR_APPLICATION_APPROVE', 'Approve instructor application', '/api/users/instructor-applications/{id}/approve', 'PUT'::permission_method, 'INSTRUCTOR_APPLICATIONS'),
        ('INSTRUCTOR_APPLICATION_REJECT', 'Reject instructor application', '/api/users/instructor-applications/{id}/reject', 'PUT'::permission_method, 'INSTRUCTOR_APPLICATIONS'),
        ('INSTRUCTOR_PROFILE_READ_OWN', 'Read own instructor profile', '/api/users/instructor-profiles/me', 'GET'::permission_method, 'INSTRUCTOR_PROFILES'),
        ('INSTRUCTOR_PROFILE_READ', 'Read instructor profile by user id', '/api/users/instructor-profiles/{userId}', 'GET'::permission_method, 'INSTRUCTOR_PROFILES'),
        ('FILE_CONTENT_READ', 'Read file content stream', '/api/files/{id}/content', 'GET'::permission_method, 'FILES'),
        ('FILE_THUMBNAIL_READ', 'Read file thumbnail stream', '/api/files/{id}/thumbnail', 'GET'::permission_method, 'FILES'),
        ('AI_QDRANT_STATS_READ', 'Read Qdrant statistics', '/api/ai/admin/qdrant-stats', 'GET'::permission_method, 'AI'),
        ('AI_RUNTIME_STATS_READ', 'Read AI runtime statistics', '/api/ai/admin/runtime-stats', 'GET'::permission_method, 'AI'),
        ('AI_DATA_CONTRACT_READ', 'Read AI data contract', '/api/ai/admin/data-contract', 'GET'::permission_method, 'AI'),
        ('AI_DATA_CONTRACT_VALIDATE', 'Validate AI data contract against database schema', '/api/ai/admin/data-contract/validate', 'GET'::permission_method, 'AI'),
        ('AI_PROVIDER_CONFIG_READ', 'Read AI provider config', '/api/ai/admin/provider-config', 'GET'::permission_method, 'AI'),
        ('AI_PROVIDER_CONFIG_UPDATE', 'Update AI provider config', '/api/ai/admin/provider-config', 'POST'::permission_method, 'AI'),
        ('AI_LANGFUSE_TRACES_READ', 'Read Langfuse traces', '/api/ai/admin/langfuse-traces', 'GET'::permission_method, 'AI'),
        ('AI_LANGFUSE_TRACE_READ', 'Read Langfuse trace detail', '/api/ai/admin/langfuse-trace/{traceId}', 'GET'::permission_method, 'AI'),
        ('AI_PROVIDER_HEALTH_READ', 'Read AI provider health', '/api/ai/admin/provider-health', 'GET'::permission_method, 'AI'),
        ('AI_AVAILABLE_MODELS_READ', 'Read available AI models', '/api/ai/admin/available-models', 'GET'::permission_method, 'AI'),
        ('AI_LANGFUSE_ANALYTICS_READ', 'Read Langfuse analytics', '/api/ai/admin/langfuse-analytics', 'GET'::permission_method, 'AI'),
        ('AI_INGEST_FILE_UPLOADED', 'Ingest uploaded file into AI index', '/api/ai/admin/ingest-file-uploaded', 'POST'::permission_method, 'AI'),
        ('REVENUE_ANALYTICS_INSTRUCTOR_OVERVIEW', 'Read instructor revenue overview', '/api/analytics/instructor/overview', 'GET'::permission_method, 'REVENUE'),
        ('REVENUE_ANALYTICS_INSTRUCTOR_TRENDS', 'Read instructor revenue trends', '/api/analytics/instructor/trends', 'GET'::permission_method, 'REVENUE'),
        ('REVENUE_ANALYTICS_ADMIN_OVERVIEW', 'Read admin revenue overview', '/api/analytics/admin/overview', 'GET'::permission_method, 'REVENUE'),
        ('REVENUE_ANALYTICS_ADMIN_TRENDS', 'Read admin revenue trends', '/api/analytics/admin/trends', 'GET'::permission_method, 'REVENUE'),
        ('REVENUE_POLICY_ACTIVE_READ', 'Read active revenue policy', '/api/payments/revenue-policies/active', 'GET'::permission_method, 'REVENUE'),
        ('REVENUE_POLICY_LIST', 'List revenue policies', '/api/payments/revenue-policies', 'GET'::permission_method, 'REVENUE'),
        ('REVENUE_POLICY_CREATE', 'Create revenue policy', '/api/payments/revenue-policies', 'POST'::permission_method, 'REVENUE'),
        ('PAYMENT_FX_RATE_READ', 'Read exchange rate', '/api/payments/fx/rate', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_FX_CONVERT_READ', 'Convert exchange rate', '/api/payments/fx/convert', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_VNPAY_CREATE_PROXY', 'Create VNPay payment through proxy', '/api/payments/vn-pay', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_PAYPAL_CREATE_PROXY', 'Create PayPal payment through proxy', '/api/payments/paypal/create', 'POST'::permission_method, 'PAYMENT'),
        ('PAYMENT_HISTORY_READ', 'Read payment history', '/api/payments/history', 'GET'::permission_method, 'PAYMENT'),
        ('PAYMENT_READ', 'Read payment detail', '/api/payments/{paymentId}', 'GET'::permission_method, 'PAYMENT'),
        ('PAYOUT_BALANCE_READ', 'Read payout balance', '/api/payments/payouts/balance', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_SUMMARY_READ', 'Read payout operations summary', '/api/payments/payouts/summary', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_CREATE', 'Create payout request', '/api/payments/payouts/requests', 'POST'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_LIST', 'List payout requests', '/api/payments/payouts/requests', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_READ', 'Read payout request detail', '/api/payments/payouts/requests/{requestId}', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_APPROVE', 'Approve payout request', '/api/payments/payouts/requests/{requestId}/approve', 'PUT'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_SETTLE', 'Settle payout request', '/api/payments/payouts/requests/{requestId}/settle', 'PUT'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_REJECT', 'Reject payout request', '/api/payments/payouts/requests/{requestId}/reject', 'PUT'::permission_method, 'PAYOUTS'),
        ('PAYOUT_REQUEST_MARK_PAID', 'Mark payout request paid', '/api/payments/payouts/requests/{requestId}/mark-paid', 'PUT'::permission_method, 'PAYOUTS'),
        ('PAYOUT_BATCH_LIST', 'List payout batches', '/api/payments/payouts/batches', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_BATCH_MONTHLY_CREATE', 'Create monthly payout batch', '/api/payments/payouts/batches/monthly', 'POST'::permission_method, 'PAYOUTS'),
        ('PAYOUT_BATCH_MANUAL_CREATE', 'Create manual payout batch', '/api/payments/payouts/batches/manual', 'POST'::permission_method, 'PAYOUTS'),
        ('PAYOUT_INVOICE_LIST', 'List payout invoices', '/api/payments/payouts/invoices', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_INVOICE_READ', 'Read payout invoice detail', '/api/payments/payouts/invoices/{invoiceId}', 'GET'::permission_method, 'PAYOUTS'),
        ('PAYOUT_INVOICE_PDF_READ', 'Read payout invoice PDF', '/api/payments/payouts/invoices/{invoiceId}/pdf', 'GET'::permission_method, 'PAYOUTS'),
        ('MANAGE_DASHBOARD_VIEW', 'Open dashboard page', '/manage/dashboard', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_AI_ANALYTICS_VIEW', 'Open AI analytics page', '/manage/ai-analytics', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_AI_TRACES_VIEW', 'Open AI traces page', '/manage/ai-traces', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_AI_PROVIDERS_VIEW', 'Open AI providers page', '/manage/ai-providers', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_REVENUE_VIEW', 'Open revenue page', '/manage/revenue', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_PAYOUTS_VIEW', 'Open payouts page', '/manage/payouts', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_ACCOUNTS_VIEW', 'Open accounts page', '/manage/accounts', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_INSTRUCTOR_APPLICATIONS_VIEW', 'Open instructor applications page', '/manage/instructor-applications', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_ROLES_VIEW', 'Open roles page', '/manage/roles', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_BLOGS_VIEW', 'Open blogs management page', '/manage/blogs', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_FILES_VIEW', 'Open files management page', '/manage/files', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_PERMISSIONS_VIEW', 'Open permissions page', '/manage/permissions', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_COURSES_VIEW', 'Open courses management page', '/manage/courses', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_COURSE_CONTENT_VIEW', 'Open course content management page', '/manage/courses/{id}/content', 'GET'::permission_method, 'MANAGE'),
        ('MANAGE_LEARNING_PATHS_VIEW', 'Open learning paths management page', '/manage/learning-paths', 'GET'::permission_method, 'MANAGE')
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

WITH ranked AS (
    SELECT ctid,
           ROW_NUMBER() OVER (PARTITION BY name ORDER BY is_active DESC, updated DESC, created DESC) AS rn
    FROM permissions
)
UPDATE permissions p
SET is_active = 'N', updated = CURRENT_TIMESTAMP
FROM ranked r
WHERE p.ctid = r.ctid AND r.rn > 1;

INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = 'Y'
  AND p.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

WITH baseline(role_name, permission_name) AS (
    VALUES
        ('INSTRUCTOR', 'USER_READ_ALL'),
        ('INSTRUCTOR', 'USER_READ'),
        ('INSTRUCTOR', 'USER_PROFILE'),
        ('INSTRUCTOR', 'USER_READ_EMAIL'),
        ('INSTRUCTOR', 'USER_READ_USERNAME'),
        ('INSTRUCTOR', 'USER_CHANGE_PASSWORD'),
        ('INSTRUCTOR', 'BLOG_READ_ALL'),
        ('INSTRUCTOR', 'BLOG_READ'),
        ('INSTRUCTOR', 'BLOG_TAGS'),
        ('INSTRUCTOR', 'BLOG_COMMENT_READ'),
        ('INSTRUCTOR', 'BLOG_COMMENT_CREATE'),
        ('INSTRUCTOR', 'COURSE_CREATE'),
        ('INSTRUCTOR', 'COURSE_READ_ALL'),
        ('INSTRUCTOR', 'COURSE_MY_COURSES'),
        ('INSTRUCTOR', 'COURSE_READ'),
        ('INSTRUCTOR', 'COURSE_UPDATE'),
        ('INSTRUCTOR', 'COURSE_DELETE'),
        ('INSTRUCTOR', 'COURSE_CHAPTER_READ'),
        ('INSTRUCTOR', 'COURSE_CHAPTER_CREATE'),
        ('INSTRUCTOR', 'COURSE_CHAPTER_UPDATE'),
        ('INSTRUCTOR', 'COURSE_CHAPTER_DELETE'),
        ('INSTRUCTOR', 'COURSE_LESSON_READ'),
        ('INSTRUCTOR', 'COURSE_LESSON_CREATE'),
        ('INSTRUCTOR', 'COURSE_LESSON_UPDATE'),
        ('INSTRUCTOR', 'COURSE_LESSON_DELETE'),
        ('INSTRUCTOR', 'COURSE_LESSON_ASSET_CREATE'),
        ('INSTRUCTOR', 'COURSE_LESSON_ASSET_UPDATE'),
        ('INSTRUCTOR', 'COURSE_LESSON_ASSET_DELETE'),
        ('INSTRUCTOR', 'COURSE_PROGRESS_READ'),
        ('INSTRUCTOR', 'COURSE_STREAK_READ'),
        ('INSTRUCTOR', 'COURSE_RATING_READ'),
        ('INSTRUCTOR', 'COURSE_RATING_CREATE'),
        ('INSTRUCTOR', 'COURSE_COMMENT_READ'),
        ('INSTRUCTOR', 'COURSE_COMMENT_CREATE'),
        ('INSTRUCTOR', 'COURSE_COMMENT_DELETE'),
        ('INSTRUCTOR', 'COURSE_LESSON_COMMENT_READ'),
        ('INSTRUCTOR', 'COURSE_LESSON_COMMENT_CREATE'),
        ('INSTRUCTOR', 'COURSE_WORKSPACE_COMMENT_READ'),
        ('INSTRUCTOR', 'COURSE_WORKSPACE_COMMENT_CREATE'),
        ('INSTRUCTOR', 'COURSE_EXERCISE_READ'),
        ('INSTRUCTOR', 'COURSE_EXERCISE_UPSERT'),
        ('INSTRUCTOR', 'COURSE_EXERCISE_SUBMIT'),
        ('INSTRUCTOR', 'COURSE_EXERCISES_READ'),
        ('INSTRUCTOR', 'COURSE_LESSON_LEADERBOARD_READ'),
        ('INSTRUCTOR', 'COURSE_EXERCISES_CREATE'),
        ('INSTRUCTOR', 'COURSE_EXERCISES_UPDATE'),
        ('INSTRUCTOR', 'COURSE_EXERCISES_DELETE'),
        ('INSTRUCTOR', 'COURSE_WORKSPACE_READ'),
        ('INSTRUCTOR', 'COURSE_WORKSPACE_SAVE'),
        ('INSTRUCTOR', 'COURSE_SKILL_CREATE'),
        ('INSTRUCTOR', 'COURSE_SKILL_READ'),
        ('INSTRUCTOR', 'COURSE_SKILL_READ_ALL'),
        ('INSTRUCTOR', 'COURSE_SKILL_UPDATE'),
        ('INSTRUCTOR', 'COURSE_SKILL_DELETE'),
        ('INSTRUCTOR', 'COURSE_TAG_CREATE'),
        ('INSTRUCTOR', 'COURSE_TAG_READ'),
        ('INSTRUCTOR', 'COURSE_TAG_READ_ALL'),
        ('INSTRUCTOR', 'COURSE_TAG_UPDATE'),
        ('INSTRUCTOR', 'COURSE_TAG_DELETE'),
        ('INSTRUCTOR', 'ENROLLMENT_CREATE'),
        ('INSTRUCTOR', 'ENROLLMENT_READ'),
        ('INSTRUCTOR', 'ENROLLMENT_MY_ENROLLMENTS'),
        ('INSTRUCTOR', 'LEARNING_PATH_CREATE'),
        ('INSTRUCTOR', 'LEARNING_PATH_READ_ALL'),
        ('INSTRUCTOR', 'LEARNING_PATH_READ'),
        ('INSTRUCTOR', 'LEARNING_PATH_UPDATE'),
        ('INSTRUCTOR', 'LEARNING_PATH_DELETE'),
        ('INSTRUCTOR', 'LEARNING_PATH_SEARCH'),
        ('INSTRUCTOR', 'LEARNING_PATH_BY_CREATOR'),
        ('INSTRUCTOR', 'LEARNING_PATH_BY_COURSE'),
        ('INSTRUCTOR', 'LEARNING_PATH_ADD_COURSES'),
        ('INSTRUCTOR', 'LEARNING_PATH_REMOVE_COURSE'),
        ('INSTRUCTOR', 'LEARNING_PATH_REORDER_COURSES'),
        ('INSTRUCTOR', 'LEARNING_PATH_PROGRESS_READ'),
        ('INSTRUCTOR', 'LEARNING_PATH_PROGRESS_BY_USER'),
        ('INSTRUCTOR', 'LEARNING_PATH_PROGRESS_BY_PATH'),
        ('INSTRUCTOR', 'LEARNING_PATH_STATISTICS'),
        ('INSTRUCTOR', 'FILE_UPLOAD'),
        ('INSTRUCTOR', 'FILE_UPLOAD_MULTIPLE'),
        ('INSTRUCTOR', 'FILE_READ'),
        ('INSTRUCTOR', 'FILE_READ_ALL'),
        ('INSTRUCTOR', 'FILE_READ_BY_FOLDER'),
        ('INSTRUCTOR', 'FILE_DELETE'),
        ('INSTRUCTOR', 'FILE_STATISTICS'),
        ('INSTRUCTOR', 'FOLDER_CREATE'),
        ('INSTRUCTOR', 'FOLDER_READ_BY_USER'),
        ('INSTRUCTOR', 'FOLDER_READ'),
        ('INSTRUCTOR', 'FOLDER_READ_TREE'),
        ('INSTRUCTOR', 'FOLDER_UPDATE'),
        ('INSTRUCTOR', 'FOLDER_DELETE'),
        ('INSTRUCTOR', 'FILE_USAGE_TRACK'),
        ('INSTRUCTOR', 'FILE_USAGE_REMOVE'),
        ('INSTRUCTOR', 'FILE_USAGE_REMOVE_ALL'),
        ('INSTRUCTOR', 'FILE_USAGE_READ'),
        ('INSTRUCTOR', 'AI_GENERATE_EXERCISES'),
        ('INSTRUCTOR', 'AI_GENERATE_LEARNING_PATH'),
        ('INSTRUCTOR', 'AI_RECOMMEND_REALTIME'),
        ('INSTRUCTOR', 'AI_RECOMMEND_SCHEDULED'),
        ('INSTRUCTOR', 'AI_RECOMMEND_HISTORY'),
        ('INSTRUCTOR', 'AI_CHAT'),
        ('INSTRUCTOR', 'AI_CHAT_STREAM'),
        ('INSTRUCTOR', 'AI_CHAT_STREAM_SIMPLE'),
        ('INSTRUCTOR', 'AI_CHAT_SESSIONS'),
        ('INSTRUCTOR', 'AI_CHAT_SESSION_CREATE'),
        ('INSTRUCTOR', 'AI_CHAT_SESSION_DETAIL'),
        ('INSTRUCTOR', 'AI_CHAT_SESSION_DELETE'),
        ('INSTRUCTOR', 'AI_CHAT_SESSION_MESSAGES'),
        ('INSTRUCTOR', 'AI_DRAFTS_EXERCISES'),
        ('INSTRUCTOR', 'AI_DRAFTS_EXERCISES_BATCH'),
        ('INSTRUCTOR', 'AI_DRAFTS_EXERCISES_LATEST'),
        ('INSTRUCTOR', 'AI_DRAFTS_DETAIL'),
        ('INSTRUCTOR', 'AI_DRAFTS_LEARNING_PATHS'),
        ('INSTRUCTOR', 'AI_DRAFTS_APPROVE_EXERCISE'),
        ('INSTRUCTOR', 'AI_DRAFTS_APPROVE_LEARNING_PATH'),
        ('INSTRUCTOR', 'AI_DRAFTS_REJECT'),
        ('INSTRUCTOR', 'NOTIFICATION_READ_ALL'),
        ('INSTRUCTOR', 'NOTIFICATION_UNREAD_COUNT'),
        ('INSTRUCTOR', 'NOTIFICATION_MARK_READ'),
        ('INSTRUCTOR', 'NOTIFICATION_MARK_ALL_READ'),
        ('INSTRUCTOR', 'PAYMENT_VNPAY_CREATE'),
        ('INSTRUCTOR', 'PAYMENT_VNPAY_CALLBACK'),
        ('INSTRUCTOR', 'PAYMENT_PAYPAL_CREATE'),
        ('INSTRUCTOR', 'PAYMENT_PAYPAL_SUCCESS'),
        ('INSTRUCTOR', 'PAYMENT_PAYPAL_CANCEL'),
        ('INSTRUCTOR', 'TRANSACTION_READ_BY_USER'),
        ('INSTRUCTOR', 'TRANSACTION_READ'),
        ('INSTRUCTOR', 'TRANSACTION_PAYMENTS_READ'),
        ('INSTRUCTOR', 'USER_EFFECTIVE_PERMISSIONS_READ'),
        ('INSTRUCTOR', 'USER_PERMISSION_CHECK'),
        ('INSTRUCTOR', 'FILE_CONTENT_READ'),
        ('INSTRUCTOR', 'FILE_THUMBNAIL_READ'),
        ('INSTRUCTOR', 'REVENUE_ANALYTICS_INSTRUCTOR_OVERVIEW'),
        ('INSTRUCTOR', 'REVENUE_ANALYTICS_INSTRUCTOR_TRENDS'),
        ('INSTRUCTOR', 'REVENUE_POLICY_ACTIVE_READ'),
        ('INSTRUCTOR', 'REVENUE_POLICY_LIST'),
        ('INSTRUCTOR', 'PAYMENT_FX_RATE_READ'),
        ('INSTRUCTOR', 'PAYMENT_FX_CONVERT_READ'),
        ('INSTRUCTOR', 'PAYMENT_HISTORY_READ'),
        ('INSTRUCTOR', 'PAYMENT_READ'),
        ('INSTRUCTOR', 'PAYOUT_BALANCE_READ'),
        ('INSTRUCTOR', 'PAYOUT_SUMMARY_READ'),
        ('INSTRUCTOR', 'PAYOUT_REQUEST_CREATE'),
        ('INSTRUCTOR', 'PAYOUT_REQUEST_LIST'),
        ('INSTRUCTOR', 'PAYOUT_REQUEST_READ'),
        ('INSTRUCTOR', 'PAYOUT_INVOICE_LIST'),
        ('INSTRUCTOR', 'PAYOUT_INVOICE_READ'),
        ('INSTRUCTOR', 'PAYOUT_INVOICE_PDF_READ'),
        ('INSTRUCTOR', 'MANAGE_REVENUE_VIEW'),
        ('INSTRUCTOR', 'MANAGE_PAYOUTS_VIEW'),
        ('INSTRUCTOR', 'MANAGE_FILES_VIEW'),
        ('INSTRUCTOR', 'MANAGE_COURSES_VIEW'),
        ('INSTRUCTOR', 'MANAGE_COURSE_CONTENT_VIEW'),
        ('INSTRUCTOR', 'INSTRUCTOR_PROFILE_READ_OWN'),
        ('INSTRUCTOR', 'INSTRUCTOR_PROFILE_READ'),
        ('LEARNER', 'INSTRUCTOR_PROFILE_READ'),
        ('INSTRUCTOR', 'MANAGE_LEARNING_PATHS_VIEW'),
        ('LEARNER', 'USER_PROFILE'),
        ('LEARNER', 'USER_CHANGE_PASSWORD'),
        ('LEARNER', 'BLOG_READ_ALL'),
        ('LEARNER', 'BLOG_READ'),
        ('LEARNER', 'BLOG_TAGS'),
        ('LEARNER', 'BLOG_COMMENT_READ'),
        ('LEARNER', 'BLOG_COMMENT_CREATE'),
        ('LEARNER', 'COURSE_READ_ALL'),
        ('LEARNER', 'COURSE_READ'),
        ('LEARNER', 'COURSE_ENROLL'),
        ('LEARNER', 'COURSE_CHAPTER_READ'),
        ('LEARNER', 'COURSE_LESSON_READ'),
        ('LEARNER', 'COURSE_PROGRESS_READ'),
        ('LEARNER', 'COURSE_STREAK_READ'),
        ('LEARNER', 'COURSE_LESSON_PROGRESS_UPDATE'),
        ('LEARNER', 'COURSE_LESSON_COMPLETE'),
        ('LEARNER', 'COURSE_RATING_READ'),
        ('LEARNER', 'COURSE_RATING_CREATE'),
        ('LEARNER', 'COURSE_COMMENT_READ'),
        ('LEARNER', 'COURSE_COMMENT_CREATE'),
        ('LEARNER', 'COURSE_LESSON_COMMENT_READ'),
        ('LEARNER', 'COURSE_LESSON_COMMENT_CREATE'),
        ('LEARNER', 'COURSE_WORKSPACE_COMMENT_READ'),
        ('LEARNER', 'COURSE_WORKSPACE_COMMENT_CREATE'),
        ('LEARNER', 'COURSE_EXERCISE_READ'),
        ('LEARNER', 'COURSE_EXERCISE_SUBMIT'),
        ('LEARNER', 'COURSE_EXERCISES_READ'),
        ('LEARNER', 'COURSE_LESSON_LEADERBOARD_READ'),
        ('LEARNER', 'COURSE_WORKSPACE_READ'),
        ('LEARNER', 'COURSE_WORKSPACE_SAVE'),
        ('LEARNER', 'COURSE_SKILL_READ'),
        ('LEARNER', 'COURSE_SKILL_READ_ALL'),
        ('LEARNER', 'COURSE_TAG_READ'),
        ('LEARNER', 'COURSE_TAG_READ_ALL'),
        ('LEARNER', 'ENROLLMENT_CREATE'),
        ('LEARNER', 'ENROLLMENT_READ'),
        ('LEARNER', 'ENROLLMENT_MY_ENROLLMENTS'),
        ('LEARNER', 'LEARNING_PATH_READ_ALL'),
        ('LEARNER', 'LEARNING_PATH_READ'),
        ('LEARNER', 'LEARNING_PATH_SEARCH'),
        ('LEARNER', 'LEARNING_PATH_BY_CREATOR'),
        ('LEARNER', 'LEARNING_PATH_BY_COURSE'),
        ('LEARNER', 'LEARNING_PATH_PROGRESS_UPSERT'),
        ('LEARNER', 'LEARNING_PATH_PROGRESS_READ'),
        ('LEARNER', 'LEARNING_PATH_PROGRESS_BY_USER'),
        ('LEARNER', 'LEARNING_PATH_STATISTICS'),
        ('LEARNER', 'FILE_UPLOAD'),
        ('LEARNER', 'FILE_UPLOAD_MULTIPLE'),
        ('LEARNER', 'FILE_READ'),
        ('LEARNER', 'FILE_READ_ALL'),
        ('LEARNER', 'FILE_READ_BY_FOLDER'),
        ('LEARNER', 'FOLDER_READ_BY_USER'),
        ('LEARNER', 'FOLDER_READ'),
        ('LEARNER', 'FOLDER_READ_TREE'),
        ('LEARNER', 'FILE_USAGE_READ'),
        ('LEARNER', 'AI_RECOMMEND_REALTIME'),
        ('LEARNER', 'AI_RECOMMEND_HISTORY'),
        ('LEARNER', 'AI_CHAT'),
        ('LEARNER', 'AI_CHAT_STREAM'),
        ('LEARNER', 'AI_CHAT_STREAM_SIMPLE'),
        ('LEARNER', 'AI_CHAT_SESSIONS'),
        ('LEARNER', 'AI_CHAT_SESSION_CREATE'),
        ('LEARNER', 'AI_CHAT_SESSION_DETAIL'),
        ('LEARNER', 'AI_CHAT_SESSION_DELETE'),
        ('LEARNER', 'AI_CHAT_SESSION_MESSAGES'),
        ('LEARNER', 'NOTIFICATION_READ_ALL'),
        ('LEARNER', 'NOTIFICATION_UNREAD_COUNT'),
        ('LEARNER', 'NOTIFICATION_MARK_READ'),
        ('LEARNER', 'NOTIFICATION_MARK_ALL_READ'),
        ('LEARNER', 'PAYMENT_VNPAY_CREATE'),
        ('LEARNER', 'PAYMENT_VNPAY_CALLBACK'),
        ('LEARNER', 'PAYMENT_PAYPAL_CREATE'),
        ('LEARNER', 'PAYMENT_PAYPAL_SUCCESS'),
        ('LEARNER', 'PAYMENT_PAYPAL_CANCEL'),
        ('LEARNER', 'TRANSACTION_READ_BY_USER'),
        ('LEARNER', 'TRANSACTION_READ'),
        ('LEARNER', 'TRANSACTION_PAYMENTS_READ'),
        ('LEARNER', 'USER_EFFECTIVE_PERMISSIONS_READ'),
        ('LEARNER', 'USER_PERMISSION_CHECK'),
        ('LEARNER', 'INSTRUCTOR_APPLICATION_SUBMIT'),
        ('LEARNER', 'INSTRUCTOR_APPLICATION_READ_OWN'),
        ('LEARNER', 'INSTRUCTOR_APPLICATION_READ'),
        ('LEARNER', 'FILE_CONTENT_READ'),
        ('LEARNER', 'FILE_THUMBNAIL_READ'),
        ('LEARNER', 'PAYMENT_FX_RATE_READ'),
        ('LEARNER', 'PAYMENT_FX_CONVERT_READ'),
        ('LEARNER', 'PAYMENT_VNPAY_CREATE_PROXY'),
        ('LEARNER', 'PAYMENT_PAYPAL_CREATE_PROXY'),
        ('LEARNER', 'PAYMENT_HISTORY_READ'),
        ('LEARNER', 'PAYMENT_READ')
), scoped_roles AS (
    SELECT id, name FROM roles WHERE name IN ('INSTRUCTOR', 'LEARNER')
), inactive AS (
    UPDATE role_permissions rp
    SET is_active = 'N', updated = CURRENT_TIMESTAMP
    FROM scoped_roles sr
    WHERE rp.role_id = sr.id
      AND NOT EXISTS (
          SELECT 1
          FROM baseline b
          JOIN permissions p ON p.name = b.permission_name
          WHERE b.role_name = sr.name
            AND p.id = rp.permission_id
      )
    RETURNING rp.role_id, rp.permission_id
)
INSERT INTO role_permissions (role_id, permission_id, is_active, granted_at, created, updated)
SELECT r.id, p.id, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM baseline b
JOIN roles r ON r.name = b.role_name AND r.is_active = 'Y'
JOIN permissions p ON p.name = b.permission_name AND p.is_active = 'Y'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = 'Y', updated = CURRENT_TIMESTAMP;

-- Normalize old broad PUBLIC policies that exposed protected modules.
WITH protected(url_pattern, method) AS (
    VALUES
        ('/api/files/**', '*'),
        ('/api/folders/**', '*'),
        ('/api/file-usage/**', '*'),
        ('/api/payment/**', '*'),
        ('/api/payments/**', '*'),
        ('/api/transactions/**', '*')
)
UPDATE endpoint_security_policies p
SET security_level = 'AUTHORIZED'::security_level,
    description = 'Protected by DB permissions',
    is_active = 'Y',
    updated = CURRENT_TIMESTAMP
FROM protected
WHERE p.url_pattern = protected.url_pattern
  AND p.method = protected.method;

WITH seed(url_pattern, method, security_level, description) AS (
    VALUES
        ('/api/v1/instructor-applications/n8n-callback', '*', 'PUBLIC'::security_level, 'N8n CV scan callback'),
        ('/api/internal/endpoint-security-policies', 'GET', 'PUBLIC'::security_level, 'Proxy policy cache feed'),
        ('/api/courses', 'GET', 'PUBLIC'::security_level, 'Public course catalog list'),
        ('/api/courses/my-courses', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated instructor course management list'),
        ('/api/courses/{id}', 'GET', 'PUBLIC'::security_level, 'Public course detail'),
        ('/api/courses/{id}/chapters', 'GET', 'PUBLIC'::security_level, 'Public course chapter outline'),
        ('/api/courses/skills', 'GET', 'PUBLIC'::security_level, 'Public course skill filters'),
        ('/api/courses/skills/{id}', 'GET', 'PUBLIC'::security_level, 'Public course skill detail'),
        ('/api/courses/tags', 'GET', 'PUBLIC'::security_level, 'Public course tag filters'),
        ('/api/courses/tags/{id}', 'GET', 'PUBLIC'::security_level, 'Public course tag detail'),
        ('/api/courses/streak', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated learning streak'),
        ('/api/courses/{id}/ratings', 'GET', 'PUBLIC'::security_level, 'Public course rating summary'),
        ('/api/courses/{id}/comments', 'GET', 'PUBLIC'::security_level, 'Public course comments'),
        ('/api/courses/{courseId}/lessons/{lessonId}/comments', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated lesson comments'),
        ('/api/courses/{courseId}/lessons/{lessonId}/leaderboard', 'GET', 'PUBLIC'::security_level, 'Public lesson leaderboard'),
        ('/api/learning-paths', 'GET', 'PUBLIC'::security_level, 'Public learning path list'),
        ('/api/learning-paths/{id}', 'GET', 'PUBLIC'::security_level, 'Public learning path detail'),
        ('/api/learning-paths/search', 'GET', 'PUBLIC'::security_level, 'Public learning path search'),
        ('/api/learning-paths/by-course/{courseId}', 'GET', 'PUBLIC'::security_level, 'Public learning paths by course'),
        ('/api/blogs', 'GET', 'PUBLIC'::security_level, 'Public blog list'),
        ('/api/blogs/{id}', 'GET', 'PUBLIC'::security_level, 'Public blog detail'),
        ('/api/blogs/tags', 'GET', 'PUBLIC'::security_level, 'Public blog tags'),
        ('/api/blogs/{id}/comments', 'GET', 'PUBLIC'::security_level, 'Public blog comments'),
        ('/api/users/instructor-applications/**', '*', 'AUTHORIZED'::security_level, 'Instructor application APIs require DB permissions'),
        ('/api/users/{userId}/permissions/**', '*', 'AUTHORIZED'::security_level, 'User permission APIs require DB permissions'),
        ('/api/users/instructor-applications', '*', 'AUTHORIZED'::security_level, 'Instructor application APIs require DB permissions'),
        ('/api/v1/instructor-applications/**', '*', 'AUTHORIZED'::security_level, 'Direct instructor application APIs require DB permissions'),
        ('/api/users/resend-reset-code/**', '*', 'PUBLIC'::security_level, 'Resend reset code'),
        ('/api/v1/instructor-applications', '*', 'AUTHORIZED'::security_level, 'Direct instructor application APIs require DB permissions'),
        ('/api/v1/instructor-profiles/me', 'GET', 'AUTHENTICATED'::security_level, 'Any logged-in user can read their own instructor profile'),
        ('/api/users/instructor-profiles/me', 'GET', 'AUTHENTICATED'::security_level, 'Any logged-in user can read their own instructor profile'),
        ('/api/v1/instructor-profiles/**', '*', 'AUTHORIZED'::security_level, 'Instructor profile APIs require DB permissions'),
        ('/api/users/instructor-profiles/{userId}', 'GET', 'PUBLIC'::security_level, 'Public instructor profile by user id'),
        ('/api/users/instructor-profiles/**', '*', 'AUTHORIZED'::security_level, 'Proxied instructor profile APIs require DB permissions'),
        ('/api/v1/payment/vn-pay-callback', 'GET', 'PUBLIC'::security_level, 'Direct VNPay callback'),
        ('/api/v1/payment/paypal/success', 'GET', 'PUBLIC'::security_level, 'Direct PayPal success callback'),
        ('/api/payments/vn-pay-callback', 'GET', 'PUBLIC'::security_level, 'VNPay callback'),
        ('/api/v1/payment/paypal/cancel', 'GET', 'PUBLIC'::security_level, 'Direct PayPal cancel callback'),
        ('/api/users/reset-password/**', '*', 'PUBLIC'::security_level, 'Reset password'),
        ('/api/payments/paypal/success', 'GET', 'PUBLIC'::security_level, 'PayPal success callback'),
        ('/api/payments/paypal/cancel', 'GET', 'PUBLIC'::security_level, 'PayPal cancel callback'),
        ('/api/users/forgot-password', '*', 'PUBLIC'::security_level, 'Forgot password'),
        ('/api/users/change-password', 'POST', 'AUTHENTICATED'::security_level, 'Change own password'),
        ('/api/v1/transactions/**', '*', 'AUTHORIZED'::security_level, 'Direct transaction APIs require DB permissions'),
        ('/api/ai/chat/stream/health', 'GET', 'PUBLIC'::security_level, 'AI chat streaming health check'),
        ('/api/ai/chat/stream', 'POST', 'AUTHORIZED'::security_level, 'AI chat streaming requires DB permission and trusted identity'),
        ('/api/ai/chat/stream/simple', 'GET', 'AUTHORIZED'::security_level, 'AI simple streaming requires DB permission and trusted identity'),
        ('/api/ai/chat/stream/**', '*', 'AUTHORIZED'::security_level, 'AI chat streaming endpoints require DB permission and trusted identity'),
        ('/api/users/public/**', '*', 'PUBLIC'::security_level, 'Public user endpoints'),
        ('/api/transactions/**', '*', 'AUTHORIZED'::security_level, 'Transaction APIs require DB permissions'),
        ('/api/users/{userId}', 'GET', 'PUBLIC'::security_level, 'Public view user by ID (author/instructor/commenter display)'),
        ('/api/users/profile', '*', 'AUTHENTICATED'::security_level, 'Current user profile'),
        ('/api/file-usage/**', '*', 'AUTHORIZED'::security_level, 'File usage APIs require DB permissions'),
        ('/api/v1/payment/**', '*', 'AUTHORIZED'::security_level, 'Direct payment APIs require DB permissions'),
        ('/api/analytics/**', '*', 'AUTHORIZED'::security_level, 'Analytics APIs require DB permissions'),
        ('/app/actuator/**', '*', 'PUBLIC'::security_level, 'Actuator through proxy'),
        ('/api/payments/**', '*', 'AUTHORIZED'::security_level, 'Payment APIs require DB permissions'),
        ('/v3/api-docs/**', '*', 'PUBLIC'::security_level, 'OpenAPI docs'),
        ('/api/folders/**', '*', 'AUTHORIZED'::security_level, 'Folder APIs require DB permissions'),
        ('/api/payment/**', '*', 'AUTHORIZED'::security_level, 'Payment APIs require DB permissions'),
        ('/swagger-ui/**', '*', 'PUBLIC'::security_level, 'Swagger UI'),
        ('/api/oauth2/**', '*', 'PUBLIC'::security_level, 'OAuth2 controller'),
        ('/api/admin/**', '*', 'AUTHORIZED'::security_level, 'Admin APIs require DB permissions'),
        ('/api/files/**', '*', 'AUTHORIZED'::security_level, 'File APIs require DB permissions'),
        ('/api/auth/**', '*', 'PUBLIC'::security_level, 'Auth endpoints'),
        ('/actuator/**', '*', 'PUBLIC'::security_level, 'Actuator health and info'),
        ('/oauth2/**', '*', 'PUBLIC'::security_level, 'OAuth2 flow'),
        ('/api/ai/**', '*', 'AUTHORIZED'::security_level, 'AI APIs require DB permissions'),
        ('/api/users', 'POST', 'PUBLIC'::security_level, 'User registration')
), updated AS (
    UPDATE endpoint_security_policies p
    SET security_level = seed.security_level,
        description = seed.description,
        is_active = 'Y',
        updated = CURRENT_TIMESTAMP
    FROM seed
    WHERE p.url_pattern = seed.url_pattern
      AND p.method = seed.method
    RETURNING p.url_pattern, p.method
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

WITH ranked AS (
    SELECT ctid,
           ROW_NUMBER() OVER (PARTITION BY url_pattern, method ORDER BY is_active DESC, updated DESC, created DESC) AS rn
    FROM endpoint_security_policies
)
UPDATE endpoint_security_policies p
SET is_active = 'N', updated = CURRENT_TIMESTAMP
FROM ranked r
WHERE p.ctid = r.ctid AND r.rn > 1;
