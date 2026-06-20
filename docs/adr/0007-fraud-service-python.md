# ADR-0007: The fraud/risk engine is a separate Python service

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

The core bank is a Java/Spring modular monolith (ADR-0001), and that decision
explicitly reserved process boundaries for the one case that genuinely justifies
them: a different runtime. Real-time risk scoring is an ML/statistics problem, and
Python's ecosystem (scikit-learn, pandas, numpy) is materially better for it than
Java's. This is the textbook "right tool for the job" boundary, not language
tourism.

We must also decide *how* the core calls it (sync vs async) and what happens when
it flags a transaction.

## Decision

Run the fraud/risk engine as a **separate Python (FastAPI) service**. The Java core
calls it **synchronously over REST** during a transfer, because the decision must
*gate* the transfer (a flagged transfer is held before it posts). The engine starts
with explainable **rules** (amount threshold, velocity, new payee) and can graduate
to a trained anomaly model later.

- **Held, not blocked:** a `HOLD` verdict records a `held_transfer` (not posted) for
  admin review; `ALLOW` posts normally. Releasing posts it; rejecting discards it.
- **Fail-open:** if the fraud service is unavailable, the transfer is allowed
  (availability over strictness for this simulation; a real bank might fail closed).
  This is logged and is a deliberate, documented trade-off.
- **Async is for notifications,** not the decision: held/completed events flow over
  RabbitMQ to the notification worker (ADR-/§ messaging). The decision stays sync
  because it must precede the posting.
- **Tracing crosses the boundary:** the Java OTel agent propagates trace context on
  the REST call, and the FastAPI service is OTel-instrumented, so one trace spans
  both services.

## Consequences

**Positive**

- The ML lives where the ML tooling is; Java keeps owning money and correctness.
- The boundary is a real runtime boundary — the honest justification for a service.
- A flagged transfer is visibly held and reviewable; the demo is compelling.
- End-to-end traces across the language boundary are a strong observability signal.

**Negative / trade-offs**

- A synchronous call on the transfer path couples transfer latency/availability to
  the fraud service; mitigated by fail-open and a fast rule engine. Moving to async
  scoring (score-after-post, then claw back) is possible but loses the clean "held
  before it moves" semantics.
- A second language/runtime to build, test, and operate.
- Signals (velocity, new payee) are computed in the core and passed in, so the
  service stays stateless — simple, but the core does that work.

## Alternatives considered

- **In-process Java fraud logic** — no extra service, but Java is the wrong tool for
  the ML trajectory, and it wouldn't demonstrate a justified polyglot boundary.
- **Async-only scoring over RabbitMQ** — non-blocking, but can't hold a transfer
  *before* it posts; you'd post then compensate. Kept for notifications, not the gate.
- **gRPC instead of REST** — typed and fast; REST is simpler and the call is not
  latency-critical. A reasonable future change.
