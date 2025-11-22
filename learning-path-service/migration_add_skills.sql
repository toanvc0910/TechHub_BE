-- Migration: Add skills column to learning_paths table
-- Date: 2025-11-22
-- Description: Add JSONB column to store array of skill names for learning paths

-- Check if column exists before adding
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'learning_paths' 
        AND column_name = 'skills'
    ) THEN
        -- Add skills column as JSONB array
        ALTER TABLE learning_paths 
        ADD COLUMN skills JSONB DEFAULT '[]'::jsonb;
        
        COMMENT ON COLUMN learning_paths.skills IS 'Array of skill names associated with this learning path';
        
        RAISE NOTICE 'Column skills added to learning_paths table';
    ELSE
        RAISE NOTICE 'Column skills already exists in learning_paths table';
    END IF;
END $$;

-- Optional: Create index on skills for better query performance
CREATE INDEX IF NOT EXISTS idx_learning_paths_skills 
ON learning_paths USING GIN (skills);

-- Optional: Update existing records to have empty array if NULL
UPDATE learning_paths 
SET skills = '[]'::jsonb 
WHERE skills IS NULL;
