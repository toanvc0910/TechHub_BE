BEGIN;

ALTER TABLE blogs
    ADD COLUMN IF NOT EXISTS tags text[] DEFAULT ARRAY[]::text[],
    ADD COLUMN IF NOT EXISTS related_course_ids uuid[] NOT NULL DEFAULT ARRAY[]::uuid[],
    ADD COLUMN IF NOT EXISTS related_lesson_ids uuid[] NOT NULL DEFAULT ARRAY[]::uuid[];

CREATE INDEX IF NOT EXISTS idx_blogs_tags_gin
    ON blogs USING gin (tags);

CREATE INDEX IF NOT EXISTS idx_blogs_related_course_ids
    ON blogs USING gin (related_course_ids);

CREATE INDEX IF NOT EXISTS idx_blogs_related_lesson_ids
    ON blogs USING gin (related_lesson_ids);

COMMIT;
