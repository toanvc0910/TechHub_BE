-- Migration script to update ai_generation_tasks table
-- Adds created_by, updated_by columns and new index for better draft querying

-- Add created_by and updated_by columns
ALTER TABLE ai_generation_tasks
    ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);

-- Add composite index for efficient draft querying
DROP INDEX IF EXISTS idx_ai_generation_tasks_target_status;
CREATE INDEX idx_ai_generation_tasks_target_status 
    ON ai_generation_tasks(target_reference, status, task_type);

-- Update default status from PENDING to DRAFT
ALTER TABLE ai_generation_tasks 
    ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Add comment for target_reference column
COMMENT ON COLUMN ai_generation_tasks.target_reference IS 
'For EXERCISE_GENERATION: stores lesson_id (UUID). For LEARNING_PATH_GENERATION: stores goal text or user_id';

-- Add check constraint for valid status values (optional, if not using ENUM)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_ai_generation_tasks_status'
    ) THEN
        ALTER TABLE ai_generation_tasks 
        ADD CONSTRAINT chk_ai_generation_tasks_status 
        CHECK (status IN ('DRAFT', 'APPROVED', 'REJECTED', 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'));
    END IF;
END $$;

-- Add trigger to auto-update 'updated' column
CREATE OR REPLACE FUNCTION update_ai_generation_tasks_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_ai_generation_tasks ON ai_generation_tasks;
CREATE TRIGGER trg_update_ai_generation_tasks
    BEFORE UPDATE ON ai_generation_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_ai_generation_tasks_timestamp();

-- Data migration: Update existing COMPLETED tasks to DRAFT if needed
-- (This is optional based on your business logic)
UPDATE ai_generation_tasks 
SET status = 'DRAFT' 
WHERE status = 'COMPLETED' 
  AND task_type IN ('EXERCISE_GENERATION', 'LEARNING_PATH_GENERATION')
  AND result_payload IS NOT NULL;

-- Verify migration
SELECT 
    task_type,
    status,
    COUNT(*) as count
FROM ai_generation_tasks
WHERE is_active = 'Y'
GROUP BY task_type, status
ORDER BY task_type, status;
