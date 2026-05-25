-- Migration: V002__collab_filtering_indexes.sql
-- Purpose:   Add indexes to support the collaborative-filtering recommendation
--            query in ai-service-fastapi (fetch_collaborative_candidates).
--
-- The 4-CTE query performs the following lookups that benefit from indexes:
--
--   1. user_courses CTE       → enrollments(user_id, is_active, status)
--   2. co_learners CTE        → enrollments(course_id) WHERE user_id != target
--   3. co_enrolled CTE        → enrollments(user_id) WHERE course_id NOT IN (...)
--   4. Final SELECT           → courses(id, is_active, status)
--                             → course_tags(course_id), course_skills(course_id)
--                             → tags(id, is_active), skills(id, is_active)

-- ---------------------------------------------------------------------------
-- enrollments
-- ---------------------------------------------------------------------------

-- Covers: WHERE user_id = :user_id AND is_active = 'Y' AND status NOT IN (...)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_enrollments_user_active_status
    ON enrollments (user_id, is_active, status);

-- Covers: WHERE course_id IN (...) AND is_active = 'Y' AND status NOT IN (...)
-- Used by co_learners CTE and co_enrolled CTE
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_enrollments_course_active_status
    ON enrollments (course_id, is_active, status);

-- ---------------------------------------------------------------------------
-- courses
-- ---------------------------------------------------------------------------

-- Covers: JOIN courses c ON c.id = ... AND c.is_active = 'Y' AND c.status = 'PUBLISHED'
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_courses_status_active
    ON courses (status, is_active);

-- ---------------------------------------------------------------------------
-- course_tags / course_skills  (junction tables)
-- ---------------------------------------------------------------------------

-- Covers: LEFT JOIN course_tags ct ON ct.course_id = ...
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_course_tags_course_id
    ON course_tags (course_id);

-- Covers: LEFT JOIN course_skills cs ON cs.course_id = ...
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_course_skills_course_id
    ON course_skills (course_id);

-- ---------------------------------------------------------------------------
-- tags / skills  (name lookup + is_active filter)
-- ---------------------------------------------------------------------------

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tags_id_active
    ON tags (id, is_active);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_skills_id_active
    ON skills (id, is_active);
