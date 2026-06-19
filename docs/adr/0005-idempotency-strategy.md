# ADR-0005: Idempotency for money-moving operations

- **Status:** Accepted
- **Date:** 2026-06-19

## Context

Clients retry. A network can drop a response *after* the server has already
committed a transfer. Without protection, the client's retry produces a second
transfer — a double charge. Money operations must therefore be **idempotent**: the
same logical request, sent more than once, must take effect at most once.

Forces:

- The guarantee must be **durable** (survive restarts), so it lives in Postgres,
  not only in memory/Redis.
- We must distinguish a genuine retry (same key, same request) from a *misuse* of a
  key (same key, different request) — the latter is a client bug and should fail
  loudly rather than silently return the wrong result.
- The first-write and the idempotency bookkeeping must be **atomic** — they either
  both commit or both roll back.

## Decision

Every money-moving operation carries a client-generated **`Idempotency-Key`**
(a UUID, sent as an HTTP header).

- A durable `idempotency_record` table stores `(key → request fingerprint, stored
  response, status)`. The key is the primary key.
- On first use: process the operation and store the result, **in the same database
  transaction** as the posting. A retry that arrives later finds the record and
  returns the **stored result without re-processing**.
- The request is fingerprinted (SHA-256 of its semantic fields). Reusing a key with
  a *different* request body is a **409 Conflict**, never a silent wrong answer.
- Defence in depth: `posting.idempotency_key` is also `UNIQUE`, so even a race that
  slipped past the record check cannot write two postings for one key — the second
  insert fails and its transaction rolls back.

## Consequences

**Positive**

- A retried request can never double-charge; this is proven by tests.
- The guarantee is durable and transactional — no partial state on failure.
- Key-reuse misuse is surfaced as a clear 409 rather than corrupting data.

**Negative / trade-offs**

- Clients must generate and send a key per operation (documented; the API rejects
  money operations without one).
- Stored responses must be serialisable; we keep them small (the posting id).
- Under concurrent first-use of the same key, one request wins and the other rolls
  back and must retry — correct, but the loser sees a transient conflict rather
  than the stored result on that first race.

## Alternatives considered

- **Redis-only idempotency** — fast, but not durable; a restart or eviction could
  drop the record and re-enable a double charge. Redis is still a fine *fast-path*
  cache in front of the durable record (a later optimisation).
- **Natural idempotency via a client-supplied transfer id as the primary key** —
  workable, but the explicit record also captures the response and request
  fingerprint, which we want for conflict detection and replay.
- **No idempotency, rely on client correctness** — unacceptable for money.
