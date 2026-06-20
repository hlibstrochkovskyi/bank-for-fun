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
| Frontend    | Next.js 16 + TypeScript, Tailwind v4, shadcn/ui, TanStack Query, Auth.js (BFF) |
| Core        | Java 21 (LTS), Spring Boot 4, Spring MVC + virtual threads |
| Persistence | PostgreSQL, Spring Data JPA / Hibernate, Flyway     |
| Auth        | Keycloak (OIDC); app is an OAuth2 resource server   |
| API docs    | springdoc-openapi (Swagger UI), RFC 7807 errors     |
| Cache/limit | Redis (rate limiting)                               |
| Fraud engine | Python + FastAPI (rule-based risk scoring)         |
| Messaging   | RabbitMQ (async domain events), Mailhog (dev SMTP)  |
| Observability | Micrometer/Prometheus, OpenTelemetry → Tempo (across both services), Loki, Grafana |
| Build       | Gradle (Kotlin DSL), wrapper-pinned                 |
| Testing     | JUnit 5, Testcontainers (real Postgres + Redis)     |
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
- **Mailhog (held-transfer emails):** http://localhost:8025
- **RabbitMQ management:** http://localhost:15672 (guest / guest)
- **Try the fraud hold:** transfer ≥ $10,000 → held for review (HTTP 202); see it in
  `GET /api/held-transfers`, the email in Mailhog, and the trace across both
  services in Grafana/Tempo. `bob` (admin) releases it via `POST /api/admin/held-transfers/{id}/release`.
- **Seed demo data through the API:** `make seed`
- **Postman:** import `infra/postman/ledger-bank.postman_collection.json`, run *Login* first

All `/api/**` endpoints require a Keycloak bearer token; a user can only access
their own accounts, and money endpoints require an `Idempotency-Key` header.

### The web app

The Next.js frontend runs on the host (it's a BFF — see
[ADR-0010](docs/adr/0010-frontend-bff.md)). With the stack up (`make up`):

```bash
cd frontend
cp .env.example .env.local      # then set AUTH_SECRET (npx auth secret)
npm install
npm run dev                     # http://localhost:3000
```

Sign in as `alice` / `password`, open accounts, deposit, and transfer. Try a
transfer ≥ $10,000 to see it **held for review**; the held-transfers page shows it.
The frontend talks only to its own `/api/bank/*` proxy, which forwards your token
to core-bank server-side — the browser never sees the access token.

### Observability

`make up` also starts the LGTM stack. A transfer is traced end-to-end and its
metrics scraped:

- **Grafana:** http://localhost:3001 (anonymous admin) — Prometheus, Tempo, Loki provisioned
- **Prometheus:** http://localhost:9090 — scrapes `core-bank:/actuator/prometheus`
- **Metrics:** `/actuator/prometheus` (e.g. `ledger_postings_total{type="TRANSFER"}`)
- **Traces:** the **OpenTelemetry Java agent** auto-instruments the app and exports
  OTLP to **Tempo**. A single transfer trace shows the whole flow end-to-end — the
  `POST /api/transfers` server span over the `SELECT ... FOR UPDATE` balance locks,
  the `INSERT`s for the posting + both ledger entries, the audit row, and the
  balance-snapshot `UPDATE`s. See [ADR-0009](docs/adr/0009-tracing-agent.md).
- **Logs:** the app emits structured (ECS JSON) logs which **promtail** ships to **Loki**.

> Verified live: a transfer traced end-to-end in Tempo, Prometheus scraping
> core-bank, custom `ledger_postings` metrics, ECS JSON logs, and the provisioned
> Grafana datasources.

## Project status

Following a phased roadmap (see [the implementation plan](docs/plan/)). **Keep
`main` always demoable.**

- [x] **Phase 0 — Foundation:** monorepo, Docker Compose + Postgres, Spring Boot
  skeleton with Flyway + Actuator health, CI, ADR-0001 / ADR-0002.
- [x] **Phase 1 — Minimum finishable core:** Keycloak auth, accounts, the
  double-entry ledger (DB-enforced balance invariant), deposit + transfer,
  derived balances, idempotency, transaction history, the concurrency invariant
  test, REST API with RFC 7807 errors, Swagger UI, seed script + Postman.
- [x] **Phase 2 — Harden & expand:** withdrawals, reversals (compensating postings,
  admin-gated), append-only audit log, date-range statements, scheduled standing
  orders, Redis rate limiting, and full observability (metrics, tracing, structured logs).
- [x] **Phase 3 — Polyglot + async:** a Python (FastAPI) fraud engine the core calls
  synchronously; flagged transfers are **held** (not posted), visible to the
  customer, and released/rejected by an admin. Domain events flow through
  **RabbitMQ** to a notification worker that emails via **Mailhog**. A single
  transfer trace spans **both services** in Tempo.
- [x] **Phase 4 — Frontend:** Next.js + TypeScript + Tailwind + shadcn/ui (Calm
  Indigo). Keycloak OIDC login via a **BFF** (tokens stay server-side), dashboard
  with a balance-trend chart, transfer flow (idempotent, with held-transfer UX),
  account detail + history + date-range statements, and a held-transfers view.
- [ ] Phase 5 — DevOps & quality polish.
- [ ] Phase 6 — Documentation & presentation.

## Repository layout

```
ledger-bank/
├── core-bank/        # Java / Spring Boot modular monolith
│   └── src/main/java/com/ledgerbank/  # accounts, ledger, payments, fraud, messaging,
│                                      # notifications, audit, statements, web, config, shared
├── fraud-service/    # Python / FastAPI risk-scoring service
├── frontend/         # Next.js + TypeScript web app (BFF to core-bank)
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


## License

TBD.
