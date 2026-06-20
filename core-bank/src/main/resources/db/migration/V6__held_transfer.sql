-- Transfers held for review when the fraud engine flags them. A held transfer has
-- NOT touched the ledger; on release it is posted, on rejection it is discarded.
CREATE TABLE held_transfer (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID             NOT NULL,
    from_account_id UUID             NOT NULL REFERENCES account(id),
    to_account_id   UUID             NOT NULL REFERENCES account(id),
    amount          BIGINT           NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)       NOT NULL,
    idempotency_key TEXT             NOT NULL,
    risk_score      DOUBLE PRECISION NOT NULL,
    reason          TEXT,
    description     TEXT,
    status          TEXT             NOT NULL DEFAULT 'PENDING_REVIEW',  -- PENDING_REVIEW, RELEASED, REJECTED
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    reviewed_at     TIMESTAMPTZ
);
CREATE INDEX idx_held_transfer_owner ON held_transfer (owner_id, created_at);
CREATE INDEX idx_held_transfer_status ON held_transfer (status);
