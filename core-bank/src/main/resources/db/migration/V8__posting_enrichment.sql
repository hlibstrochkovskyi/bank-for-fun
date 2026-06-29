-- Display enrichment for a posting: a derived merchant name and spending category.
-- Additive and append-only — it never mutates the immutable posting/ledger rows. The
-- category is derived by a deterministic rule-based categorizer at posting time;
-- this table exists so spending can be aggregated in SQL (category breakdowns).
CREATE TABLE posting_enrichment (
    posting_id UUID PRIMARY KEY REFERENCES posting(id),
    merchant   TEXT,
    category   TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_posting_enrichment_category ON posting_enrichment (category);
