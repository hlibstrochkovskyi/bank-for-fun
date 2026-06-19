-- Reversals are new compensating postings, never edits/deletes. A REVERSAL posting
-- records the inverse of an original posting and links back to it. A posting may be
-- reversed at most once (partial unique index; many NULLs allowed for non-reversals).
ALTER TABLE posting ADD COLUMN reverses_posting_id UUID REFERENCES posting(id);

CREATE UNIQUE INDEX uq_posting_reverses
    ON posting (reverses_posting_id)
    WHERE reverses_posting_id IS NOT NULL;
