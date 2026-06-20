# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

`ledger-bank` — a simulated retail bank built on an **immutable double-entry
ledger**. Learning + portfolio project. Backend-first, UI later. The full plan
lives in `docs/plan/IMPLEMENTATION_PLAN (1).md`; read it before non-trivial work.

The signal of this project is **correctness, proof, and documented judgment**, not
feature count. The four things that matter most:

1. Money is an immutable double-entry ledger; **balances are derived**, never a
   mutable column that gets `UPDATE`d.
2. Concurrency is **proven** by a money-conservation test under load.
3. Money endpoints are **idempotent**.
4. Decisions are recorded as **ADRs** in `docs/adr/` — write them as you go.

## Non-negotiable domain rules

- **Never use `float`/`double` for money.** Amounts are signed integer minor units
  (`BIGINT`), wrapped in a `Money` value object carrying a currency. See
  [ADR-0002](docs/adr/0002-money-representation.md).
- **Ledger rows are immutable.** Never `UPDATE`/`DELETE` a posting or ledger entry.
  Corrections are *new compensating postings*.
- **Every posting's signed entries sum to exactly zero** (enforced in the DB).
- Rate math (interest/fees) uses `BigDecimal` with `HALF_EVEN` rounding, then posts
  the rounded minor-unit result.
- Flyway owns all DDL; Hibernate runs with `ddl-auto: validate` and never creates
  schema.

## Layout

```
core-bank/   Java 21 / Spring Boot 4 modular monolith (Gradle Kotlin DSL)
docs/adr/    Architecture Decision Records (+ index, template)
docs/plan/   the implementation plan
docker-compose.yml, Makefile, .env.example
```

Target core-bank modules (bounded contexts, enforced later via ArchUnit):
`identity, accounts, ledger, payments, statements, notifications, audit`, plus
`shared` (the `Money` value object / common kernel). Cross-module calls go through
public APIs only.

## Environment & commands

- **Java 21** is pinned via `mise` (`mise.toml`). If `java`/`gradle` aren't found,
  run commands through mise (`mise exec -- ...`) or activate it in your shell.
- Build tool is the **Gradle wrapper** in `core-bank/` (no system Gradle/Maven).

```bash
make up        # docker compose: Postgres + core-bank (built from source)
make down      # stop the stack
make test      # core-bank/gradlew test  (Testcontainers -> needs Docker)
make build     # core-bank/gradlew clean bootJar
make help      # all targets
```

From inside `core-bank/`: `./gradlew test`, `./gradlew bootTestRun` (boots the app
against a throwaway Testcontainers Postgres — handy without docker-compose).

## Testing

Integration tests use **Testcontainers against real Postgres**, not H2/mocks
(`TestcontainersConfiguration` + `@ServiceConnection`). The flagship test (Phase 1)
is the concurrency money-conservation test — keep it front and center.

## Conventions

- Commits: short, informative, imperative subject. **Do not** add Claude as a
  co-author. Keep `main` always demoable; finish a working slice before expanding.
- Java style follows `.editorconfig` (tabs, 4). Spotless/Checkstyle arrive in Phase 5.
- Match complexity to the problem. Advanced tooling that exists only to learn it is
  flagged `[learning-driven]` in the plan — don't add it as a default.

## Current status

Phases 0–3 complete. Phase 3 added: a **Python (FastAPI) fraud-service** (rule-based
scoring) the core calls synchronously; flagged transfers are **held** (not posted),
visible to the customer, and released/rejected by an admin (`held_transfer`, V6);
**RabbitMQ** carries domain events (`DomainEventForwarder`, AFTER_COMMIT) to a
**notification worker** that emails via **Mailhog**; and the trace spans both
services (Python is OTel-instrumented). ADRs 0001–0007, 0009 written.

Tracing note: core-bank's outbound fraud call uses **Apache HttpClient5** (the JDK
HttpClient drops the body under the OTel agent + virtual threads). Java fraud tests
mock `FraudClient`; `RestFraudClientTest` exercises the real HTTP serialization.

Module packages under `com.ledgerbank`: `accounts`, `ledger`, `payments`, `fraud`,
`messaging`, `notifications`, `statements`, `standingorders`, `audit`, `ratelimit`,
`web`, `config`, `shared`.

Next: Phase 4 — Next.js + TypeScript frontend.
