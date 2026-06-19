-- Standing orders: recurring transfers a customer schedules. A background scheduler
-- executes the ones whose next_run_at has passed, then advances next_run_at by the
-- interval. Execution reuses the idempotent transfer path so a double-fire is safe.
CREATE TABLE standing_order (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID        NOT NULL,
    from_account_id UUID        NOT NULL REFERENCES account(id),
    to_account_id   UUID        NOT NULL REFERENCES account(id),
    amount          BIGINT      NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)  NOT NULL,
    description     TEXT,
    interval_days   INT         NOT NULL CHECK (interval_days > 0),
    next_run_at     TIMESTAMPTZ NOT NULL,
    status          TEXT        NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, PAUSED, CANCELLED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_standing_order_due ON standing_order (status, next_run_at);
CREATE INDEX idx_standing_order_owner ON standing_order (owner_id);
