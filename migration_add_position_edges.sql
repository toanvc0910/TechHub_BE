-- Migration: Add position and layout_edges support for Learning Path Designer
-- Date: 2025-11-18

-- Step 1: Add layout_edges column to learning_paths table
ALTER TABLE learning_paths 
ADD COLUMN IF NOT EXISTS layout_edges JSONB DEFAULT '[]'::JSONB;

-- Step 2: Create index for layout_edges
CREATE INDEX IF NOT EXISTS idx_learning_paths_layout_edges_gin 
ON learning_paths USING GIN (layout_edges);

-- Step 3: Add position columns to learning_path_courses table
ALTER TABLE learning_path_courses 
ADD COLUMN IF NOT EXISTS position_x INTEGER,
ADD COLUMN IF NOT EXISTS position_y INTEGER,
ADD COLUMN IF NOT EXISTS is_optional VARCHAR(1) DEFAULT 'N' CHECK (is_optional IN ('Y', 'N'));

-- Step 4: Create index for position columns
CREATE INDEX IF NOT EXISTS idx_path_courses_position 
ON learning_path_courses(position_x, position_y);

-- Verify changes
SELECT 
    column_name, 
    data_type, 
    column_default,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'learning_paths' 
  AND column_name IN ('layout_edges')
ORDER BY ordinal_position;

SELECT 
    column_name, 
    data_type, 
    column_default,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'learning_path_courses' 
  AND column_name IN ('position_x', 'position_y', 'is_optional')
ORDER BY ordinal_position;

-- Check existing data
SELECT id, title, layout_edges FROM learning_paths LIMIT 5;
SELECT path_id, course_id, "order", position_x, position_y, is_optional FROM learning_path_courses LIMIT 10;
