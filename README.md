# Funny Bank

A simulated retail bank built on a proper double-entry ledger. It has accounts,
deposits, transfers, withdrawals, statements, a debit-card model, savings goals,
a small fraud engine, and a web UI you can actually click around in.

## Read this first: what this is, and what it isn't

This is a **personal portfolio and learning project.** I built it to get hands-on
with the parts of a money system that are genuinely hard to get right, and to
practice writing things down as I go. It is not a product, it is not deployed
anywhere, and you should not move real money with it.

It's a **simulation.** Specifically, these things are faked or skipped on purpose:

- No real payment rails — no ACH/SWIFT/SEPA, no card networks. A "deposit" just
  creates money out of a system account; nothing leaves or enters the real world.
- No KYC/AML, no real onboarding, no credit checks. Users come from a demo
  Keycloak realm with two hardcoded logins.
- The "debit card" is a visual + a record; it doesn't authorize anything.
- The fraud engine is a handful of `if` statements, not a trained model.
- Security is "good enough to be correct," not hardened for hostile internet
  traffic (no secrets management, fixed dev passwords, etc.).

What I *did* try to get right is the stuff that's easy to fake in a demo and
expensive to fix later:

1. **Money is an immutable double-entry ledger, not a balance column.** Balances
   are derived. The database itself refuses to store an unbalanced transaction.
2. **It's correct under concurrency, and there's a test that proves it.** Thousands
   of overlapping transfers, and money is still conserved to the cent afterward.
3. **Money operations are idempotent.** Retrying a request can't double-charge.
4. **The decisions are written down** as short [ADRs](docs/adr/), so the "why" is
   recoverable, not just the "what."

If you only look at one file, make it
[`LedgerService.java`](core-bank/src/main/java/com/ledgerbank/ledger/LedgerService.java)
and the [ledger schema](core-bank/src/main/resources/db/migration/V2__ledger_schema.sql).
That's the heart of it.

---

## The core idea: a double-entry ledger

Most toy "bank" projects keep a `balance` column on an account and do
`UPDATE accounts SET balance = balance - 100` on a transfer. That works until two
requests touch the same row at once, or one of them half-fails, and now your books
are wrong and you can't tell what happened.

Real ledgers don't store balances. They store **entries**, and the balance is just
the sum of the entries. Money moving is always recorded as a **posting**: a group
of entries whose signed amounts add up to exactly zero. Money is never created or
destroyed; it only moves between accounts.

Sign convention here: an entry's `amount` is **signed minor units** (cents). A
debit is negative, a credit is positive.

A **transfer of $25.00 from A to B** is one posting with two entries:

| posting | account | amount (cents) | meaning |
|---------|---------|---------------:|---------|
| TRANSFER | A | −2500 | money leaves A |
| TRANSFER | B | +2500 | money lands in B |
|          |   | **0** | balanced ✓ |

A **deposit** is the same shape. There's no magic "money from nowhere" — every
deposit pulls from a **system clearing account** that represents the outside world:

| posting | account | amount | meaning |
|---------|---------|-------:|---------|
| DEPOSIT | system clearing | −5000 | the outside world pays in |
| DEPOSIT | your checking   | +5000 | you receive it |

The clearing account is allowed to go negative (it's modelling "everywhere
else"); customer accounts are not. Because every posting sums to zero, the sum of
**all** entries across the whole database is always zero. That single fact is the
invariant everything else is built to protect.

---

## How a transfer actually flows, end to end

This is the path of one `POST /api/transfers` when you click "Send transfer" in
the UI:

```
Browser (Next.js)
  │  POST /api/bank/transfers   (no token in the browser)
  ▼
Next.js BFF proxy  (route handler)
  │  attaches the user's access token server-side, forwards to:
  ▼
core-bank  POST /api/transfers   (Spring MVC, OAuth2 resource server validates the JWT)
  │
  ├─ TransferController → PaymentsService.transfer(...)
  │     │
  │     ├─ IdempotencyService: have we seen this Idempotency-Key before?
  │     │     • yes, same request  → return the stored result, do nothing
  │     │     • yes, different body → 409 conflict
  │     │     • no                 → run the action (below), record the result
  │     │
  │     ├─ Fraud screen: synchronous HTTP call to the Python fraud-service
  │     │     • score ≥ 0.7 → don't post; create a HELD transfer, return 202
  │     │     • otherwise   → continue
  │     │
  │     └─ LedgerService.record(posting):
  │           1. validate the legs sum to zero
  │           2. SELECT ... FOR UPDATE the affected balance rows, in sorted order
  │           3. check no account would drop below its floor (no overdraft)
  │           4. INSERT the posting + both ledger entries  (immutable)
  │           5. UPDATE both balance snapshots in the same transaction
  │           6. publish an in-process MoneyPostedEvent
  │
  ├─ COMMIT  (the DB's deferred trigger re-checks "entries sum to zero" here)
  │
  └─ after commit: if a transfer was held, DomainEventForwarder publishes a
       TransferHeldEvent to RabbitMQ → a notifications worker emails you (Mailhog)
```

The whole thing is one database transaction (steps 1–6 plus the idempotency
record). If anything throws, it all rolls back together — no half-written
postings, no orphaned idempotency records.

---

## The pieces

### core-bank — a modular monolith (Java 21 / Spring Boot)

One deployable app, split into modules by domain. Modules talk to each other
through public service classes, not by reaching into each other's tables. It's a
monolith on purpose ([ADR-0001](docs/adr/0001-modular-monolith.md)): for a
solo project, microservices would be mostly overhead. The module boundaries are
real, though, so it *could* be split later.

| Module | Job |
|--------|-----|
| `shared` | The `Money` value object and cross-module events. The common kernel. |
| `accounts` | Open/close accounts, account types (checking, savings, system). |
| `ledger` | The only code that moves money. Postings, entries, balances, locking. |
| `payments` | Orchestrates deposits/withdrawals/transfers/reversals on top of the ledger. Idempotency lives here. |
| `fraud` | Client to the Python risk service + the "allow everything" fallback. |
| `cards` | Issue a debit card bound to an account (presentational; spending would post to that account). |
| `goals` | Savings goals — a target on a savings account; progress is the real balance. |
| `statements` | Date-range statements: opening/closing balance, totals, the entries between. |
| `standingorders` | Scheduled recurring transfers, run by a scheduler. |
| `enrichment` | Tags postings with a spending category (groceries, rent, …) for the UI. |
| `audit` | Append-only log of sensitive actions. |
| `notifications` | Consumes domain events off RabbitMQ and sends email. |
| `messaging` | Bridges in-process events to RabbitMQ after commit. |
| `ratelimit` | Per-user request limiting backed by Redis. |
| `web` | REST controllers, request/response DTOs, the global error handler. |
| `config` | Security, OpenAPI, scheduling, web config. |

### The ledger internals

Five tables ([V2 migration](core-bank/src/main/resources/db/migration/V2__ledger_schema.sql)):

- **`account`** — who owns it, type, currency, status.
- **`posting`** — one financial event (a deposit, a transfer…). Has a `type` and a
  **unique** `idempotency_key`. Immutable.
- **`ledger_entry`** — the actual money. `(posting_id, account_id, amount, currency)`,
  where `amount` is signed cents and `CHECK (amount <> 0)`. Immutable. **This is the
  source of truth.**
- **`account_balance`** — a *cached* snapshot of each account's balance, updated in
  the same transaction as the entries. Also holds `min_balance` (the overdraft
  floor; `NULL` = unbounded, used for system accounts) and a `version` column. You
  can always throw this table away and rebuild it by re-summing `ledger_entry`.
- **`idempotency_record`** — durable record of money requests we've already handled.

**Immutability is a rule, not a convention.** Postings and entries are never
`UPDATE`d or `DELETE`d. A mistake is fixed by writing a *new* compensating posting
(a reversal), so the history stays complete and auditable.

**The invariant is enforced by the database, not just the app.** There's a
deferred constraint trigger (`assert_posting_balanced`) that fires at commit and
rejects any posting whose entries don't sum to zero per currency, or that has
fewer than two entries. It's deferred so all of a posting's entries exist when it
runs. Even if the application code had a bug, Postgres would refuse to store
broken books.

### `Money` — never a float

[`Money`](core-bank/src/main/java/com/ledgerbank/shared/Money.java) is an immutable
value object: a `long` of signed minor units plus a `Currency`. No `float`/`double`
anywhere near money ([ADR-0002](docs/adr/0002-money-representation.md)). Arithmetic
uses `Math.addExact`/`subtractExact`, so overflow throws loudly instead of silently
wrapping, and every operation checks both sides share a currency — you can't add
USD to EUR. Building from a decimal (e.g. `12.34`) throws if you hand it more
precision than the currency allows, rather than quietly rounding.

### Concurrency, and the test that proves it

The dangerous moment is two transfers touching the same account at the same time.
The approach ([ADR-0004](docs/adr/0004-pessimistic-locking.md)) is pessimistic
locking with a twist:

1. Figure out every account a posting touches.
2. Lock their balance rows with `SELECT ... FOR UPDATE` — **sorted by account id.**
   Locking in a deterministic order is what makes deadlocks impossible (two
   transfers between the same pair of accounts can't grab the locks in opposite
   orders).
3. Under that lock, check no account would drop below its floor, then write the
   entries and move the snapshots.

The evidence this works is
[`MoneyConservationConcurrencyTest`](core-bank/src/test/java/com/ledgerbank/ledger/MoneyConservationConcurrencyTest.java):
it seeds 20 accounts, then fires **8 threads × 250 random transfers** at them, and
afterward asserts four things:

1. `SUM(amount)` over every ledger entry is exactly `0` (double-entry held globally).
2. The customer balances still sum to the original seed total (nothing created or
   destroyed).
3. No account went below its minimum (the overdraft floor held under contention).
4. The cached snapshot equals the balance re-derived from entries (the cache never
   drifted).

Transfers that exceed the source balance are *expected* to be refused — the test
counts those rejections separately rather than treating them as failures.

### Idempotency

Money endpoints require an `Idempotency-Key` header
([ADR-0005](docs/adr/0005-idempotency-strategy.md)).
[`IdempotencyService`](core-bank/src/main/java/com/ledgerbank/payments/IdempotencyService.java)
wraps each operation:

- First time we see a key: run the action, store its result + a SHA-256 hash of the
  request, all in the **same transaction** as the posting. Success is durably
  recorded; failure leaves no trace.
- Replay with the same key and same body: return the stored result, don't run again.
- Same key, *different* body: that's a bug on the caller's side → `409 Conflict`.
- Two requests racing with the same key: the `UNIQUE` constraint on the record lets
  exactly one win; the loser rolls back (posting and all) and the client retries to
  get the real result.

### The fraud engine (a separate Python service)

A small **FastAPI** service ([`fraud-service/`](fraud-service/)) that core-bank calls
**synchronously** during a transfer ([ADR-0007](docs/adr/0007-fraud-service-python.md)).
It's deliberately one polyglot service to practice the cross-language,
cross-service tracing story — not because a bank needs Python here.

The scoring ([`scoring.py`](fraud-service/app/scoring.py)) is stateless and
explainable: the caller passes the amount and a couple of signals (is this a new
payee, how many recent transfers), rules add up to a `0..1` score, and `≥ 0.7`
means **HOLD**. The main rule: a transfer ≥ $10,000 scores 0.8 on its own, so any
five-figure transfer gets held. That's the easiest one to demo.

When a transfer is held, **it is not posted.** No money moves. Instead a
`held_transfer` row is created, the customer sees it in the UI ("we're reviewing
this — the money hasn't moved"), and an **admin** (the `bob` user) can release it
(which posts it for real) or reject it. If the fraud service is down or disabled,
core-bank falls back to an "allow everything" client so the bank keeps working.

> Gotcha I hit: under the OpenTelemetry Java agent + virtual threads, the JDK
> `HttpClient` dropped the request body on the outbound fraud call. Switching that
> one client to Apache HttpClient5 fixed it. The Java fraud tests mock the client;
> one test (`RestFraudClientTest`) exercises the real HTTP serialization.

### Async events and notifications

Two kinds of events:

- **In-process** (`MoneyPostedEvent`, `TransferHeldEvent`) published via Spring's
  `ApplicationEventPublisher` while handling a request.
- **Cross-service**: a
  [`DomainEventForwarder`](core-bank/src/main/java/com/ledgerbank/messaging/DomainEventForwarder.java)
  listens for those events **`AFTER_COMMIT`** and republishes them to **RabbitMQ.**
  After-commit matters: you only want to tell the world about a hold once it's
  actually durable. If publishing fails, it's logged, not thrown — the business
  transaction already succeeded.

A **notifications worker** consumes `TransferHeldEvent` off RabbitMQ and sends an
email through **Mailhog** (a fake SMTP server with a web inbox at :8025). So the
flow is: held transfer → committed → event → queue → worker → email you can read.

### The rest of the features

- **Reversals** — fix a posting by recording its exact inverse as a new `REVERSAL`
  posting (admin-gated). A posting can be reversed at most once, and a reversal
  can't itself be reversed. Reversals bypass the overdraft floor, since the
  correction may legitimately push an account negative.
- **Statements** — pick a date range, get opening/closing balances, total
  credits/debits, and the entries in between. The "balance as of" a point in time
  is re-derived from the immutable entries, not read from a snapshot.
- **Standing orders** — recurring scheduled transfers, executed by a Spring scheduler.
- **Enrichment** — postings get tagged with a spending category so the dashboard
  can show a breakdown.
- **Cards / savings goals** — a debit-card record bound to an account, and savings
  targets whose progress is just the account's real balance.
- **Rate limiting** — a `@RateLimited` annotation + interceptor backed by Redis,
  limiting requests per user.

### Auth

Keycloak ([ADR-0006](docs/adr/0006-keycloak-auth.md)) is the identity provider
(OIDC). core-bank is a **stateless OAuth2 resource server**
([`SecurityConfig`](core-bank/src/main/java/com/ledgerbank/config/SecurityConfig.java)):
every `/api/**` request must carry a valid Keycloak-issued JWT; health, metrics,
and the API docs are public. Realm roles from the token become Spring authorities,
so `/api/admin/**` requires the `admin` role. On top of that, a user can only
touch their own accounts — ownership is checked in the service layer, not just by
role.

### Frontend (Next.js + TypeScript)

A Next.js web app that talks to core-bank through a **backend-for-frontend (BFF)**
([ADR-0010](docs/adr/0010-frontend-bff.md)). The important property: **the browser
never holds the access token.** You log in via Keycloak through Auth.js; the token
lives in a server-side session; and the browser calls the app's own
[`/api/bank/*` proxy](frontend/src/app/api/bank/[...path]/route.ts), which attaches
the token and forwards to core-bank server-side. Data fetching/caching is TanStack
Query.

The UI covers the dashboard (balance trend, spending breakdown, recent activity),
transfers (with the idempotency key generated client-side and the held-transfer
UX), account detail/history/statements, cards, savings, and an admin review queue.

> Sign-out does a full **RP-initiated logout** — it ends the Keycloak SSO session,
> not just the local cookie, so signing out and back in actually re-prompts instead
> of silently logging you back in.

On the visual side there are two themes living on different branches: a calm
editorial look ("Laurel") on `main`, and a full **8-bit / NES** pixel theme on the
`retro-8bit` branch. Same app, very different paint.

### Observability

`make up` also brings up the Grafana **LGTM** stack, and a transfer is observable
three ways:

- **Metrics** — Micrometer → Prometheus at `/actuator/prometheus`, including custom
  counters like `ledger_postings_total{type="TRANSFER"}`.
- **Traces** — the **OpenTelemetry Java agent** auto-instruments core-bank and
  exports to **Tempo** ([ADR-0009](docs/adr/0009-tracing-agent.md)). One transfer
  trace shows the server span over the `SELECT ... FOR UPDATE` locks, the entry
  `INSERT`s, the balance `UPDATE`s — **and** the span continuing into the Python
  fraud service, because it's OTel-instrumented too.
- **Logs** — structured ECS JSON logs that promtail ships to **Loki.**

---

## Tech stack

| Area | Choice |
|------|--------|
| Frontend | Next.js 16 + TypeScript, Tailwind v4, shadcn/Base UI, TanStack Query, Auth.js (BFF) |
| Core API | Java 21, Spring Boot 4, Spring MVC on virtual threads ([ADR-0003](docs/adr/0003-mvc-virtual-threads.md)) |
| Persistence | PostgreSQL 17, Spring Data JPA / Hibernate, Flyway for all DDL (Hibernate is `validate`-only) |
| Auth | Keycloak (OIDC); core-bank is an OAuth2 resource server |
| Fraud engine | Python + FastAPI, rule-based scoring |
| Messaging | RabbitMQ (async domain events), Mailhog (dev SMTP inbox) |
| Cache / limits | Redis (rate limiting) |
| API docs | springdoc-openapi (Swagger UI), RFC 7807 problem-detail errors |
| Observability | Micrometer/Prometheus, OpenTelemetry → Tempo, Loki, Grafana |
| Build | Gradle (Kotlin DSL), wrapper-pinned; Java pinned via `mise` |
| Testing | JUnit 5, Testcontainers (real Postgres + Redis, not H2/mocks) |
| Local infra | Docker Compose |

---

## Repository layout

```
funny-bank/
├── core-bank/            Java / Spring Boot modular monolith
│   └── src/main/java/com/ledgerbank/
│       ├── shared/       Money + cross-module events
│       ├── accounts/     accounts & account types
│       ├── ledger/       postings, entries, balances, locking  ← the core
│       ├── payments/     deposit/withdraw/transfer/reverse + idempotency
│       ├── fraud/        client to the Python risk service
│       ├── cards/ goals/ standingorders/ enrichment/ statements/
│       ├── messaging/ notifications/   RabbitMQ bridge + email worker
│       ├── audit/ ratelimit/
│       └── web/ config/  REST controllers, security, OpenAPI
│   └── src/main/resources/db/migration/   Flyway V1..V10
├── fraud-service/        Python / FastAPI risk scoring
├── frontend/             Next.js web app (BFF to core-bank)
├── infra/
│   ├── keycloak/         realm export (users alice, bob)
│   ├── observability/    Prometheus/Tempo/Loki/Grafana config
│   ├── postman/          API collection
│   ├── seed.sh           seed demo data through the real API
│   └── seed-history.sql  backdate demo timestamps so trends look alive
├── docs/
│   ├── adr/              Architecture Decision Records
│   └── plan/             the implementation plan
├── docker-compose.yml
├── Makefile
└── mise.toml             pins Java 21
```

---

## Running it

Prerequisites: **Docker** (with Compose) and **JDK 21**. Java is pinned via
[mise](https://mise.jdx.dev) (`mise.toml`); `mise install` provisions it.

```bash
make up        # build & start the whole stack (Postgres, Keycloak, core-bank, fraud, RabbitMQ, LGTM…)
make seed      # create demo accounts + a month of activity through the real API
make test      # run the core-bank test suite (Testcontainers spins up real Postgres; Docker required)
make down      # stop everything
make clean     # stop and delete volumes (wipes local data)
make help      # all targets
```

The web app runs on the host:

```bash
cd frontend
cp .env.example .env.local      # then set AUTH_SECRET (npx auth secret)
npm install
npm run dev                     # http://localhost:3000
```

Sign in as **`alice` / `password`** (regular user) or **`bob` / `password`**
(admin). Open accounts, deposit, transfer. **Try a transfer ≥ $10,000** to see it
held for review; then sign in as `bob` to release or reject it from the admin
queue.

### Poking at the API directly

Once `make up` is healthy:

| Thing | Where |
|-------|-------|
| Swagger UI | http://localhost:8080/swagger-ui.html (click *Authorize*, paste a token) |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Keycloak | http://localhost:8088 (admin `admin` / `admin`) |
| Mailhog (held-transfer emails) | http://localhost:8025 |
| RabbitMQ management | http://localhost:15672 (guest / guest) |
| Grafana | http://localhost:3001 (anonymous admin) |
| Prometheus | http://localhost:9090 |

Every `/api/**` call needs a Keycloak bearer token, a user only sees their own
accounts, and money endpoints need an `Idempotency-Key`. There's a Postman
collection in `infra/postman/` — run *Login* first.

---

## Testing approach

Integration tests run against a **real Postgres** (and Redis) via Testcontainers,
not H2 or mocks — the DB-enforced invariant and the `FOR UPDATE` locking only mean
something against the real database. Coverage spans the ledger schema and service,
payments, reversals, held transfers, the REST API, admin endpoints, rate limiting,
metrics, audit, statements, standing orders, messaging, notifications, and the
real fraud HTTP serialization.

The one to look at is the concurrency conservation test described above. It's the
whole point: not "the happy path works" but "money is still conserved after
thousands of overlapping transfers."

---

## Design decisions (ADRs)

The reasoning lives in [`docs/adr/`](docs/adr/). Short version:

| ADR | Decision |
|-----|----------|
| [0001](docs/adr/0001-modular-monolith.md) | Modular monolith over microservices (for a solo project) |
| [0002](docs/adr/0002-money-representation.md) | Money as signed integer minor units, never floats |
| [0003](docs/adr/0003-mvc-virtual-threads.md) | Spring MVC on virtual threads (not WebFlux) |
| [0004](docs/adr/0004-pessimistic-locking.md) | Pessimistic row locks + deterministic lock order |
| [0005](docs/adr/0005-idempotency-strategy.md) | Durable idempotency records keyed by header |
| [0006](docs/adr/0006-keycloak-auth.md) | Keycloak OIDC; app is a resource server |
| [0007](docs/adr/0007-fraud-service-python.md) | One justified polyglot service for fraud |
| [0009](docs/adr/0009-tracing-agent.md) | Tracing via the OpenTelemetry Java agent |
| [0010](docs/adr/0010-frontend-bff.md) | Frontend as a BFF; token stays server-side |

---

## What I'd do next

The backend (correctness, concurrency, async, observability) and the frontend are
done. Remaining on my own roadmap:

- **Quality/DevOps polish:** real CI gates (coverage, static analysis, dependency
  + container scans), ArchUnit tests to enforce the module boundaries, and a proper
  load test with a short write-up.
- **Docs:** C4 diagrams and a dedicated write-up of the ledger + concurrency
  design, since that's the most interesting part.

## License

TBD.
