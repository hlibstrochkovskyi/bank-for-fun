-- The double-entry ledger. Postings and entries are IMMUTABLE: once written they
-- are never updated or deleted; corrections are new compensating postings.
--
-- Sign convention: ledger_entry.amount is signed minor units. A debit is negative,
-- a credit is positive. Every posting's entries sum to exactly zero per currency.

-- Customer-facing and system accounts.
CREATE TABLE account (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id    UUID,                                  -- NULL for system accounts
    type        TEXT        NOT NULL,                  -- CHECKING, SAVINGS, SYSTEM_CLEARING, ...
    currency    VARCHAR(3)     NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- A posting groups balanced entries into one atomic financial event. Immutable.
CREATE TABLE posting (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            TEXT        NOT NULL,              -- DEPOSIT, TRANSFER, WITHDRAWAL, REVERSAL, ...
    idempotency_key TEXT        NOT NULL UNIQUE,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The source of truth for money. Immutable. amount is signed minor units.
CREATE TABLE ledger_entry (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    posting_id UUID        NOT NULL REFERENCES posting(id),
    account_id UUID        NOT NULL REFERENCES account(id),
    amount     BIGINT      NOT NULL CHECK (amount <> 0),
    currency   VARCHAR(3)     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_entry_account ON ledger_entry (account_id, id);
CREATE INDEX idx_entry_posting ON ledger_entry (posting_id);

-- Derived snapshot of each account's balance, updated in the SAME transaction as
-- the entries that change it. A cache of the ledger, reconcilable by re-summing.
CREATE TABLE account_balance (
    account_id  UUID PRIMARY KEY REFERENCES account(id),
    currency    VARCHAR(3)     NOT NULL,
    balance     BIGINT      NOT NULL DEFAULT 0,        -- minor units
    -- Lowest balance the account may hold; enforced under the same row lock as the
    -- balance update. NULL means unbounded (system accounts model the outside world
    -- and may go negative). Customer accounts default to 0 (no overdraft).
    min_balance BIGINT,
    version     BIGINT      NOT NULL DEFAULT 0,        -- optimistic-lock guard
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Durable idempotency record for money-moving requests.
CREATE TABLE idempotency_record (
    key          TEXT PRIMARY KEY,
    request_hash TEXT        NOT NULL,
    response     JSONB,
    status       TEXT        NOT NULL,                 -- IN_PROGRESS, COMPLETED
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Database-enforced invariant: every posting's entries must sum to zero per
-- currency, and a posting needs at least two entries. The check is DEFERRED to
-- commit so all of a posting's entries are present when it runs. The database
-- itself then refuses to store an unbalanced posting, independent of app code.
CREATE OR REPLACE FUNCTION assert_posting_balanced() RETURNS trigger AS $$
DECLARE
    entry_count INT;
BEGIN
    SELECT count(*) INTO entry_count FROM ledger_entry WHERE posting_id = NEW.posting_id;
    IF entry_count < 2 THEN
        RAISE EXCEPTION 'Posting % must have at least two entries, has %',
            NEW.posting_id, entry_count USING ERRCODE = 'check_violation';
    END IF;

    IF EXISTS (
        SELECT 1 FROM ledger_entry
        WHERE posting_id = NEW.posting_id
        GROUP BY currency
        HAVING sum(amount) <> 0
    ) THEN
        RAISE EXCEPTION 'Posting % is unbalanced: entries do not sum to zero per currency',
            NEW.posting_id USING ERRCODE = 'check_violation';
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_posting_balanced
    AFTER INSERT ON ledger_entry
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
    EXECUTE FUNCTION assert_posting_balanced();
