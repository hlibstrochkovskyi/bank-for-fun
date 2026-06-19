-- V1 baseline migration.
-- Phase 0 establishes the Flyway mechanism; the double-entry ledger schema
-- (account, posting, ledger_entry, account_balance, idempotency_record)
-- arrives in Phase 1.
--
-- pgcrypto provides gen_random_uuid() for server-side UUID generation.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
