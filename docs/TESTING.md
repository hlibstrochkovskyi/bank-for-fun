# Testing & CI pipeline

How this project is tested, and how to run / imitate every layer yourself.

## The test pyramid

| Layer | Where | What it covers | Speed |
|---|---|---|---|
| **Unit** | `core-bank` (JUnit), `fraud-service` (pytest) | Pure logic: `Money`, posting balance, overdraft, scoring rules | ms |
| **Integration** | `core-bank` (Testcontainers) | Real Postgres / Redis / RabbitMQ: persistence, locking, idempotency, audit, messaging, the **money-conservation concurrency test** | sec |
| **API / web layer** | `core-bank` (MockMvc + mock JWT) | Controllers, authz (401/403), RFC 7807 errors, held-transfer flow | sec |
| **End-to-end** | `infra/e2e-smoke.sh` | The whole stack through the frontend BFF: OIDC login → money ops → fraud hold | sec |

There is no mocking of infrastructure in integration tests — they run against real
containers, which is far more credible than H2/mocks.

## CI pipeline (`.github/workflows/ci.yml`)

Runs on every push to `main` and every PR. Two jobs:

```
core-bank · build & test     →  ./gradlew build      (compile + ALL tests, Testcontainers)
fraud-service · test         →  uv run --frozen pytest
```

`./gradlew build` is the real gate: it compiles and runs the entire suite
(82 tests) against real Postgres/Redis/RabbitMQ that Testcontainers starts on the
runner — including the concurrency test that fires thousands of parallel transfers
and asserts money is conserved to the cent.

## Run each layer locally

```bash
# Java core — compile + full suite (needs Docker for Testcontainers)
cd core-bank && ./gradlew build          # or: ./gradlew test
./gradlew test --tests "*MoneyConservationConcurrencyTest"   # the flagship test

# Python fraud service
cd fraud-service && uv run --frozen pytest -q

# Frontend — type-check + lint + production build
cd frontend && npm run build && npm run lint
```

## End-to-end (browser-equivalent) — `infra/e2e-smoke.sh`

This is the one to imitate for e2e tests. It logs in through Keycloak exactly like
a browser (CSRF → authorize → login form → callback → session cookie), then drives
**every money operation through the frontend's BFF** (`/api/bank/*`), asserting the
results — including that a ≥ $10,000 transfer is **held** by the fraud engine.

```bash
make up                                  # full stack, healthy
cd frontend && npm run dev               # frontend on :3000 (separate terminal)
./infra/e2e-smoke.sh                     # runs the scripted login + flow, asserts each step
# Env overrides: APP=... KC=... LB_USER=alice LB_PASS=password ./infra/e2e-smoke.sh
```

It exits non-zero on the first failed assertion, so it works as a CI/e2e check too.
A future hardening step is to run this in GitHub Actions against a Compose stack, or
to replace it with Playwright for real browser assertions.

## Manual demo checklist (UI)

1. Sign in at http://localhost:3000 as `alice` / `password`.
2. **Open account** → Checking; **Open account** → Savings.
3. Open the checking account → **Deposit** $1,000.
4. **Transfer** $250 checking → savings → completes.
5. **Transfer ≥ $10,000** → **held for review** (HTTP 202); appears on the **Held** page.
6. The held-transfer email lands in **Mailhog** (http://localhost:8025).
7. The transfer trace spans **both services** in **Grafana/Tempo** (http://localhost:3001).
8. `bob` / `password` (admin) can release a held transfer via the API/admin endpoint.
