-- Add status column to submissions table
ALTER TABLE submissions ADD COLUMN status submission_status NOT NULL DEFAULT 'PENDING';

-- Add index for status column
CREATE INDEX idx_submissions_status ON submissions(status);
