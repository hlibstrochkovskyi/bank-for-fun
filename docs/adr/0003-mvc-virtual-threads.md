# ADR-0003: Spring MVC + virtual threads over WebFlux

- **Status:** Accepted
- **Date:** 2026-06-19

## Context

Spring offers two web stacks: the classic servlet stack (Spring MVC, blocking) and
the reactive stack (WebFlux, non-blocking). The usual argument for WebFlux is
scalability under many concurrent connections without a thread-per-request cost.

Forces here:

- The workload is **database-bound transactional money movement**, not high-fanout
  I/O orchestration. The hard part is correctness and locking, not connection count.
- Reactive code is **harder to read, debug, and reason about** (no straightforward
  stack traces, coloured functions, tricky transaction and ThreadLocal semantics).
- **Java 21 virtual threads** (Project Loom) remove the historic reason to go
  reactive: blocking code on virtual threads scales to many concurrent requests
  without a large pool of OS threads.

## Decision

Use **Spring MVC (servlet stack) with virtual threads enabled**
(`spring.threads.virtual.enabled=true` on Java 21). Write straightforward blocking
code; let the runtime provide the scalability.

## Consequences

**Positive**

- Simple, readable, debuggable blocking code with normal stack traces.
- Transactions, `@Transactional`, JDBC, and `SELECT ... FOR UPDATE` all behave the
  obvious way — important for the ledger's correctness.
- Virtual threads give us high concurrency without a reactive rewrite.

**Negative / trade-offs**

- Not the "trendiest" choice; a reviewer expecting reactive might ask why. (The
  answer — match complexity to the problem — is the point.)
- Care needed with `synchronized` blocks that pin virtual threads; we avoid holding
  locks across blocking calls in hot paths.

## Alternatives considered

- **WebFlux (reactive)** — deliberately rejected. It buys little for a DB-bound
  transactional app and costs a lot in complexity. Would reconsider for a genuinely
  streaming or massively-fanout workload.
- **Servlet stack with a large OS-thread pool (no virtual threads)** — fine, but
  virtual threads scale better at no readability cost on Java 21.
