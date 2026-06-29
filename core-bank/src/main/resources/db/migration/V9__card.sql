-- A payment card bound to a customer account. We store only the last four digits
-- and metadata (never a full PAN) — the card is a presentation/identity object on
-- top of the account; money still lives in the ledger.
CREATE TABLE card (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID        NOT NULL REFERENCES account(id),
    owner_id    UUID        NOT NULL,
    cardholder  TEXT        NOT NULL,
    network     TEXT        NOT NULL,                 -- VISA, MASTERCARD
    last4       VARCHAR(4)  NOT NULL,
    exp_month   INT         NOT NULL,
    exp_year    INT         NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, FROZEN, CANCELLED
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_card_owner ON card (owner_id);
CREATE INDEX idx_card_account ON card (account_id);
