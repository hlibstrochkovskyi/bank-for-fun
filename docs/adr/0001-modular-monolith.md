# ADR-0001: Modular monolith over microservices

- **Status:** Accepted
- **Date:** 2026-06-19

## Context

The core of this project is a retail bank: accounts, an immutable double-entry
ledger, and money operations (deposit, transfer, withdrawal). These concerns are
tightly coupled around a single transactional consistency boundary — a transfer
must atomically lock balances, write balanced ledger entries, and update derived
snapshots within one database transaction.

We must choose an overall system shape. The fashionable default is microservices,
but the actual forces here are:

- **Strong consistency is the whole point.** Money correctness wants ACID
  transactions, not eventual consistency across service boundaries.
- **This is a learning + portfolio project with zero real users.** There is no
  scaling pressure that microservices would relieve.
- **Distributed systems carry a real tax:** network failures, distributed
  transactions/sagas, service discovery, partial failures, and cross-service
  tracing — complexity that would dominate the project without improving the
  thing that matters (a correct ledger).

## Decision

We will build the core bank as a **modular monolith**: a single Spring Boot
deployable, internally partitioned into bounded contexts (`identity`, `accounts`,
`ledger`, `payments`, `statements`, `notifications`, `audit`) with explicit public
APIs. Cross-module calls go through those APIs, never into another module's
internals. The boundaries are enforced by **ArchUnit** tests (added in a later
phase) so the seams stay real rather than aspirational.

Exactly **one** component is split out as a separate service — the ML-based
fraud/risk engine — because it has a genuinely different runtime and language
(Python). A different runtime is the legitimate reason to cross a process
boundary (see the planned ADR-0007).

## Consequences

**Positive**

- Money operations stay inside one ACID transaction — the simplest correct design.
- Clean module boundaries teach bounded-context design without the distributed tax.
- The seams are drawn so the monolith *could* be split later; knowing where the
  seams are and choosing not to cut them is stronger signal than cutting prematurely.
- One deployable is trivial to run locally (`docker compose up`) and to reason about.

**Negative / trade-offs**

- All modules share one database and one deployment lifecycle; a single module's
  heavy load or a bad migration affects the whole app.
- Module isolation is enforced by discipline + tests, not by the network. If the
  ArchUnit rules are not maintained, boundaries can erode.

**Revisit if:** a module develops genuinely independent scaling needs, a separate
team must own it, or it requires a different runtime — the same test that justified
splitting out the fraud engine.

## Alternatives considered

- **Microservices from the start** — rejected. Buys distributed-systems complexity
  with no payoff at this scale, and works against the strong consistency the ledger
  needs. The honest framing: "I know how to draw the boundaries; at this scale I'd
  run a monolith — and I did."
- **Single unstructured monolith (no module boundaries)** — rejected. Cheap now,
  but loses the design signal and tends toward a big ball of mud.
- **Spring Modulith** — a reasonable way to formalise modules; may be adopted later
  as a complement to ArchUnit. Not required to start.
