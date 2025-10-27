-- ===========================
-- FILE MANAGEMENT SYSTEM
-- ===========================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Drop existing objects if they exist
DROP TABLE IF EXISTS file_usage CASCADE;
DROP TABLE IF EXISTS files CASCADE;
DROP TABLE IF EXISTS file_folders CASCADE;
DROP TYPE IF EXISTS file_type_enum CASCADE;
DROP FUNCTION IF EXISTS update_updated() CASCADE;
DROP FUNCTION IF EXISTS get_folder_path(UUID) CASCADE;
DROP FUNCTION IF EXISTS update_folder_path() CASCADE;

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
    size BIGINT NOT NULL,
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