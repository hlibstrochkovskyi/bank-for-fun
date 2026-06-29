-- Spread the demo postings across the last ~30 days so the balance trend looks
-- alive instead of a flat line that spikes "today". This changes timestamps ONLY —
-- amounts, entries, and balances are untouched, so the ledger stays consistent and
-- balances still reconcile. Demo-data convenience; never run against real data.
WITH ordered AS (
    SELECT id,
           row_number() OVER (ORDER BY created_at, id) - 1 AS rn,
           count(*) OVER ()                               AS n
    FROM posting
    WHERE type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'REVERSAL')
)
UPDATE posting p
SET created_at = now() - interval '30 days'
              + (o.rn::numeric / GREATEST(o.n - 1, 1)) * interval '29 days'
FROM ordered o
WHERE p.id = o.id;

-- Keep each entry's timestamp in step with its posting (history/statement queries
-- order and filter by ledger_entry.created_at).
UPDATE ledger_entry le
SET created_at = p.created_at
FROM posting p
WHERE le.posting_id = p.id
  AND p.type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'REVERSAL');
