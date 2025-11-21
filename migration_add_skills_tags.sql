-- ===========================
-- MIGRATION: Add Skills & Tags System
-- Date: 2025-11-21
-- ===========================

-- Add ENUM for skill category
CREATE TYPE skill_category AS ENUM('LANGUAGE', 'FRAMEWORK', 'TOOL', 'CONCEPT', 'OTHER');

-- ===========================
-- CREATE NEW TABLES
-- ===========================

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

-- ===========================
-- JOIN TABLES (Many-to-Many)
-- ===========================

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

-- ===========================
-- TRIGGERS FOR NEW TABLES
-- ===========================

CREATE TRIGGER trg_update_skills 
BEFORE UPDATE ON skills 
FOR EACH ROW EXECUTE PROCEDURE update_updated();

CREATE TRIGGER trg_update_tags 
BEFORE UPDATE ON tags 
FOR EACH ROW EXECUTE PROCEDURE update_updated();

-- ===========================
-- MIGRATION: Remove old array columns
-- ===========================

-- Drop old indexes that reference array columns
DROP INDEX IF EXISTS idx_courses_categories_gin;
DROP INDEX IF EXISTS idx_courses_tags_gin;
DROP INDEX IF EXISTS idx_blogs_tags_gin;
DROP INDEX IF EXISTS idx_learning_paths_skills_gin;

-- Drop old columns from courses table
ALTER TABLE courses DROP COLUMN IF EXISTS categories;
ALTER TABLE courses DROP COLUMN IF EXISTS tags;

-- Drop old columns from blogs table  
ALTER TABLE blogs DROP COLUMN IF EXISTS tags;

-- Drop old columns from learning_paths table
ALTER TABLE learning_paths DROP COLUMN IF EXISTS skills;

-- ===========================
-- SAMPLE DATA (Optional)
-- ===========================

-- Insert sample skills
ALTER TABLE course_tags DROP CONSTRAINT course_tags_pkey;
ALTER TABLE course_tags ADD COLUMN id UUID PRIMARY KEY DEFAULT uuid_generate_v4();
ALTER TABLE course_tags ADD CONSTRAINT uq_course_tags UNIQUE (course_id, tag_id);
-- 1. Xóa Primary Key cũ của course_skills
ALTER TABLE course_skills DROP CONSTRAINT course_skills_pkey;

-- 2. Thêm cột id và đặt làm Primary Key mới
ALTER TABLE course_skills ADD COLUMN id UUID PRIMARY KEY DEFAULT uuid_generate_v4();

-- 3. Thêm ràng buộc UNIQUE
ALTER TABLE course_skills ADD CONSTRAINT uq_course_skills UNIQUE (course_id, skill_id);