-- Customer-facing account metadata: a human nickname and a display account number.
-- Additive only — existing rows and system accounts keep these NULL. No ledger row
-- is touched.
ALTER TABLE account ADD COLUMN nickname       TEXT;
ALTER TABLE account ADD COLUMN account_number TEXT;

CREATE UNIQUE INDEX idx_account_number ON account (account_number) WHERE account_number IS NOT NULL;
