-- Normalize RBAC and endpoint security so DB is the source of truth.
-- Safe to run multiple times against a database created from techhub.sql.

BEGIN;

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
        ('FILE_CONTENT_READ', 'Read file content stream', '/api/files/{id}/content', 'GET'::permission_method, 'FILES'),
        ('FILE_THUMBNAIL_READ', 'Read file thumbnail stream', '/api/files/{id}/thumbnail', 'GET'::permission_method, 'FILES'),
        ('AI_QDRANT_STATS_READ', 'Read Qdrant statistics', '/api/ai/admin/qdrant-stats', 'GET'::permission_method, 'AI'),
        ('AI_RUNTIME_STATS_READ', 'Read AI runtime statistics', '/api/ai/admin/runtime-stats', 'GET'::permission_method, 'AI'),
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
        ('/api/courses', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course catalog list'),
        ('/api/courses/{id}', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course detail'),
        ('/api/courses/{id}/chapters', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course chapter outline'),
        ('/api/courses/skills', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course skill filters'),
        ('/api/courses/skills/{id}', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course skill detail'),
        ('/api/courses/tags', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course tag filters'),
        ('/api/courses/tags/{id}', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course tag detail'),
        ('/api/courses/{id}/ratings', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course rating summary'),
        ('/api/courses/{id}/comments', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated course comments'),
        ('/api/courses/{courseId}/lessons/{lessonId}/comments', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated lesson comments'),
        ('/api/learning-paths', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated learning path list'),
        ('/api/learning-paths/{id}', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated learning path detail'),
        ('/api/learning-paths/search', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated learning path search'),
        ('/api/learning-paths/by-course/{courseId}', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated learning paths by course'),
        ('/api/blogs', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated blog list'),
        ('/api/blogs/{id}', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated blog detail'),
        ('/api/blogs/tags', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated blog tags'),
        ('/api/blogs/{id}/comments', 'GET', 'AUTHENTICATED'::security_level, 'Authenticated blog comments'),
        ('/api/users/instructor-applications/**', '*', 'AUTHORIZED'::security_level, 'Instructor application APIs require DB permissions'),
        ('/api/users/{userId}/permissions/**', '*', 'AUTHORIZED'::security_level, 'User permission APIs require DB permissions'),
        ('/api/users/instructor-applications', '*', 'AUTHORIZED'::security_level, 'Instructor application APIs require DB permissions'),
        ('/api/v1/instructor-applications/**', '*', 'AUTHORIZED'::security_level, 'Direct instructor application APIs require DB permissions'),
        ('/api/users/resend-reset-code/**', '*', 'PUBLIC'::security_level, 'Resend reset code'),
        ('/api/v1/instructor-applications', '*', 'AUTHORIZED'::security_level, 'Direct instructor application APIs require DB permissions'),
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
        ('/api/users/{userId}', 'GET', 'AUTHENTICATED'::security_level, 'View user by ID'),
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

COMMIT;
