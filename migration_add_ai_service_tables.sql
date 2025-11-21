-- AI Service Migration - ALTER existing database (không mất data)
-- Copy-paste toàn bộ script này vào psql và Enter

-- 1. Create ENUMs (chỉ chạy nếu chưa có)
DO $$ BEGIN
    CREATE TYPE ai_task_type AS ENUM('EXERCISE_GENERATION', 'LEARNING_PATH', 'RECOMMENDATION_REALTIME', 'RECOMMENDATION_SCHEDULED', 'CHAT_GENERAL', 'CHAT_ADVISOR');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE ai_task_status AS ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'DRAFT');
EXCEPTION WHEN duplicate_object THEN null;
END $$;

-- 2. Create table (chỉ chạy nếu chưa có)
CREATE TABLE IF NOT EXISTS ai_generation_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_type ai_task_type NOT NULL,
    status ai_task_status NOT NULL DEFAULT 'PENDING',
    target_reference VARCHAR(255),
    model_used VARCHAR(128),
    prompt TEXT,
    request_payload JSONB,
    result_payload JSONB,
    error_message TEXT,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
);

-- 3. Create indexes (chỉ chạy nếu chưa có)
CREATE INDEX IF NOT EXISTS idx_ai_generation_tasks_task_type ON ai_generation_tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_ai_generation_tasks_status ON ai_generation_tasks(status);
CREATE INDEX IF NOT EXISTS idx_ai_generation_tasks_target_reference ON ai_generation_tasks(target_reference);
CREATE INDEX IF NOT EXISTS idx_ai_generation_tasks_created ON ai_generation_tasks(created);
CREATE INDEX IF NOT EXISTS idx_ai_generation_tasks_is_active ON ai_generation_tasks(is_active);

-- 4. Create trigger (chỉ chạy nếu chưa có)
DO $$ BEGIN
    CREATE TRIGGER trg_update_ai_generation_tasks 
    BEFORE UPDATE ON ai_generation_tasks 
    FOR EACH ROW EXECUTE PROCEDURE update_updated();
EXCEPTION WHEN duplicate_object THEN null;
END $$;
