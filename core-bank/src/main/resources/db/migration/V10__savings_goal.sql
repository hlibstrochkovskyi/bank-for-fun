-- A savings goal attached to a savings account. Progress is DERIVED from the
-- account's real ledger balance — the goal stores only a name and a target, never a
-- separate balance. One goal per account.
CREATE TABLE savings_goal (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id   UUID        NOT NULL UNIQUE REFERENCES account(id),
    owner_id     UUID        NOT NULL,
    name         TEXT        NOT NULL,
    target_minor BIGINT      NOT NULL CHECK (target_minor > 0),
    currency     VARCHAR(3)  NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_savings_goal_owner ON savings_goal (owner_id);
