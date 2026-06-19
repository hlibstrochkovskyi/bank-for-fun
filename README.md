# ledger-bank

> A simulated retail bank built on a correct, immutable **double-entry ledger** —
> with idempotent money operations, proven concurrency guarantees, observability,
> and a modern UI.

This is a **learning + portfolio** project. It is a *simulation*: no real KYC/AML,
no real payment rails (SWIFT/ACH/SEPA), no card networks. The point is to get the
hard, easy-to-fake parts right — correctness, concurrency, and judgment — and to
document the reasoning.

## What makes it different

Most "bank app" projects keep a mutable `balance` column and `UPDATE` it on every
transfer. That is not how money systems work. Here:

1. **Money is an immutable double-entry ledger, not a mutable balance.** Balances
   are *derived* from ledger entries. The global invariant — *the signed sum of all
   ledger entries is always exactly zero* — is enforced by the database.
2. **Concurrency is proven, not assumed.** A test fires thousands of concurrent
   transfers and asserts money is conserved to the cent.
3. **Money operations are idempotent.** A retried request can never double-charge.
4. **Decisions are documented** as [ADRs](docs/adr/) — the reasoning is the point.

## Architecture (target)

A **modular monolith** (Java / Spring Boot) for the core bank — `identity`,
`accounts`, `ledger`, `payments`, `statements`, `notifications`, `audit` — plus one
justified separate service (a Python fraud/risk engine), plus a Next.js frontend.
See [ADR-0001](docs/adr/0001-modular-monolith.md) for why a monolith.

## Tech stack

| Area        | Choice                                              |
|-------------|-----------------------------------------------------|
| Core        | Java 21 (LTS), Spring Boot 4, Spring MVC + virtual threads |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate, Flyway     |
| Auth        | Keycloak (OIDC); app is an OAuth2 resource server   |
| API docs    | springdoc-openapi (Swagger UI), RFC 7807 errors     |
| Build       | Gradle (Kotlin DSL), wrapper-pinned                 |
| Testing     | JUnit 5, Testcontainers (real Postgres)             |
| Local infra | Docker Compose                                      |

## Quickstart

Prerequisites: **Docker** (with Compose) and **JDK 21**. The Java version is pinned
via [mise](https://mise.jdx.dev) (`mise.toml`); `mise install` provisions it.

```bash
# Boot the whole stack (Postgres + core-bank), built from source:
make up

# Health check (waits for the app):
curl -fsS http://localhost:8080/actuator/health

# Run the test suite (Testcontainers spins up a real Postgres; Docker required):
make test

# Stop everything:
make down
```

Run `make help` for all commands.

### Exploring the API

The stack includes Keycloak (realm `ledger-bank`, demo users `alice` / `bob`,
password `password`). Once `make up` is healthy:

- **Swagger UI:** http://localhost:8080/swagger-ui.html (click *Authorize* and paste a token)
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Keycloak:** http://localhost:8088 (admin `admin` / `admin`)
- **Seed demo data through the API:** `make seed`
- **Postman:** import `infra/postman/ledger-bank.postman_collection.json`, run *Login* first

All `/api/**` endpoints require a Keycloak bearer token; a user can only access
their own accounts, and money endpoints require an `Idempotency-Key` header.

## Project status

Following a phased roadmap (see [the implementation plan](docs/plan/)). **Keep
`main` always demoable.**

- [x] **Phase 0 — Foundation:** monorepo, Docker Compose + Postgres, Spring Boot
  skeleton with Flyway + Actuator health, CI, ADR-0001 / ADR-0002.
- [x] **Phase 1 — Minimum finishable core:** Keycloak auth, accounts, the
  double-entry ledger (DB-enforced balance invariant), deposit + transfer,
  derived balances, idempotency, transaction history, the concurrency invariant
  test, REST API with RFC 7807 errors, Swagger UI, seed script + Postman.
- [ ] Phase 2 — Harden & expand (reversals, statements, rate limiting, observability, audit).
- [ ] Phase 3 — Polyglot fraud engine + async messaging.
- [ ] Phase 4 — Next.js frontend.
- [ ] Phase 5 — DevOps & quality polish.
- [ ] Phase 6 — Documentation & presentation.

## Repository layout

```
ledger-bank/
├── core-bank/        # Java / Spring Boot modular monolith
│   └── src/main/java/com/ledgerbank/  # accounts, ledger, payments, web, config, shared
├── infra/
│   ├── keycloak/     # realm export
│   ├── postman/      # API collection
│   └── seed.sh       # seed demo data through the API
├── docs/
│   ├── adr/          # Architecture Decision Records
│   └── plan/         # the implementation plan
├── .github/workflows/ # CI
├── docker-compose.yml
└── Makefile
```

Future homes (added with their phases): `fraud-service/` (Python), `frontend/`
(Next.js), `infra/observability/`.

## License

TBD.
