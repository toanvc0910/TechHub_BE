-- One-off migration: convert uniq_file_folders_user_name_parent to a PARTIAL
-- unique index that only applies to ACTIVE folders.
--
-- Why: the old unique index covered every row regardless of is_active, so when
-- a folder was soft-deleted (is_active = 'N') and the user then tried to
-- create a folder with the same name + parent, the application-level check
-- passed (no active row) but the INSERT hit the unique constraint and
-- bubbled up as 409 Conflict.
--
-- The Java service is also updated to revive a soft-deleted row instead of
-- inserting a duplicate, but this migration is still required so the DB will
-- accept the recreate.
--
-- Run this against the file-service database before deploying the new build.
-- Safe to run multiple times.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = current_schema()
          AND indexname  = 'uniq_file_folders_user_name_parent'
    ) THEN
        DROP INDEX uniq_file_folders_user_name_parent;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uniq_file_folders_user_name_parent
    ON file_folders(user_id, name, COALESCE(parent_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE is_active = 'Y';
