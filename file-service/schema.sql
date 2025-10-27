-- Run this script in techhub_blog database
-- CREATE TABLE for file metadata (used by file-service)

CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGSERIAL PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    file_type VARCHAR(50) NOT NULL,
    size BIGINT NOT NULL,
    format VARCHAR(50),
    folder VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_file_metadata_public_id ON file_metadata(public_id);
CREATE INDEX IF NOT EXISTS idx_file_metadata_folder ON file_metadata(folder);
CREATE INDEX IF NOT EXISTS idx_file_metadata_created_at ON file_metadata(created_at);