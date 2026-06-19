# ADR-0004: Pessimistic locking with deterministic lock ordering

- **Status:** Accepted
- **Date:** 2026-06-19

## Context

Two transfers that touch the same account at the same instant must not corrupt its
balance or let it breach its overdraft floor. This is the part most "bank app"
projects get wrong. We need a concurrency-control strategy for the balance updates
that is correct under real contention.

Forces:

- Correctness is non-negotiable: no lost updates, no illegal overdraft, snapshot
  balances must stay reconcilable with the ledger entries.
- The contention pattern is short transactions touching a small number of rows.
- We want a strategy that is simple to reason about and easy to *prove* with a test.

## Decision

Use **pessimistic locking with deterministic lock ordering**:

- Within the transfer transaction, `SELECT ... FOR UPDATE` the affected
  `account_balance` rows before reading or changing them.
- Always acquire those locks in a **consistent order — sorted by account id** — so
  two transfers touching the same pair of accounts can never deadlock.
- The overdraft floor is checked **under the lock**, before any write, so the
  check-then-act is race-free.
- `READ COMMITTED` isolation is sufficient *because* the row locks serialise the
  conflicting updates.

An `@Version` column on `account_balance` provides a secondary optimistic guard
(belt and suspenders); it never trips in normal operation because the row lock
already serialises writers.

## Consequences

**Positive**

- No lost updates and no illegal overdraft under concurrency — proven by the
  money-conservation test (thousands of parallel transfers conserve money to the
  cent and never overdraw).
- Deadlocks are structurally impossible thanks to the global lock order.
- Simple mental model: lock, check, write, commit.

**Negative / trade-offs**

- Under very high contention on a single hot account, transfers serialise (lower
  throughput on that row). Acceptable — correctness first, and real hot-account
  contention is rare at this scale.
- Holding row locks means transactions must stay short (they do).

## Alternatives considered

- **Optimistic locking only** (`@Version` + retry loop) — better under low
  contention, but needs a retry loop and can livelock under high contention.
  Rejected as the primary mechanism; kept as a secondary guard.
- **`SERIALIZABLE` isolation with serialization-failure retries** — strong and
  elegant, but pushes a retry loop into every caller and can abort transactions
  under load. More surprising than explicit row locks for this use case.
- **Application-level locks / a single writer** — does not scale and reintroduces
  the very coordination problems the database already solves.
