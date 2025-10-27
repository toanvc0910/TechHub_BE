-- Create database
CREATE DATABASE techhub_file;

-- Connect to database
\c techhub_file;

-- Create file_metadata table
CREATE TABLE file_metadata (
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
CREATE INDEX idx_file_metadata_public_id ON file_metadata(public_id);
CREATE INDEX idx_file_metadata_folder ON file_metadata(folder);
CREATE INDEX idx_file_metadata_created_at ON file_metadata(created_at);

-- Grant permissions (optional, adjust username as needed)
-- GRANT ALL PRIVILEGES ON DATABASE techhub_file TO your_username;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_username;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO your_username;
