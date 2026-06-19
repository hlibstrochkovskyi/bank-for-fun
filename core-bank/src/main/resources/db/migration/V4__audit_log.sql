-- Append-only audit log: one row per state-changing action. Never updated or
-- deleted — a database trigger enforces this, so the log is tamper-evident in
-- spirit even against the application's own DB user.
CREATE TABLE audit_log (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id    UUID,                              -- the acting user, or NULL for system actions
    action      TEXT        NOT NULL,              -- ACCOUNT_OPENED, MONEY_POSTED, ...
    target_type TEXT        NOT NULL,              -- ACCOUNT, POSTING, ...
    target_id   TEXT,
    detail      JSONB
);
CREATE INDEX idx_audit_target ON audit_log (target_type, target_id);
CREATE INDEX idx_audit_actor ON audit_log (actor_id, id);

CREATE OR REPLACE FUNCTION forbid_audit_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only; % is not allowed', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION forbid_audit_mutation();
