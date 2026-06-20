# ADR-0009: Distributed tracing via the OpenTelemetry Java agent

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

We want distributed tracing: a single transfer should be traceable end-to-end
(HTTP → service → database), exported to Tempo and viewable in Grafana.

The Spring-native approach is in-process: Micrometer Observation + the
`micrometer-tracing` OpenTelemetry bridge, auto-configured by
`spring-boot-starter-opentelemetry`, exporting OTLP. We tried this first.

On **Spring Boot 4.1.0** (with micrometer-tracing 1.7 / OpenTelemetry 1.62) it does
not work: the `Tracer`, OTLP exporter, propagator, and tracing
`ObservationHandler` beans are all created, but **Boot does not attach the tracing
handlers to the `ObservationRegistry`**, so HTTP-request observations never become
spans and nothing is exported. (Metrics observations work — the meter handler *is*
attached.) This was confirmed by a diagnostic test: manually attaching the handler
beans to the registry immediately produces valid, sampled spans. It appears to be a
wiring/ordering issue in this just-released version combination.

## Decision

Use the **OpenTelemetry Java agent** for tracing instead of the in-process bridge.
The agent (`-javaagent`, attached via `JAVA_TOOL_OPTIONS` in `docker-compose`)
auto-instruments servlets, JDBC, HTTP clients, etc. at the bytecode level and
exports OTLP to Tempo, configured entirely through `OTEL_*` environment variables.

- The agent jar is baked into the runtime image; it is only active when the env
  vars are present, so tests/CI run without it.
- Metrics stay on **Prometheus** (`OTEL_METRICS_EXPORTER=none`); logs stay on the
  structured console → Loki (`OTEL_LOGS_EXPORTER=none`). The agent does tracing only.
- `spring-boot-starter-opentelemetry` is removed; no in-process tracing code remains.

## Consequences

**Positive**

- Tracing works: a transfer trace shows the HTTP span over the `SELECT ... FOR
  UPDATE` locks, the posting/entry `INSERT`s, the audit row, and the snapshot
  `UPDATE`s — verified live in Tempo.
- The agent is the production-standard way to instrument the JVM; richer
  out-of-the-box coverage (JDBC, clients) than hand-placed observations.
- No application code or dependency coupling to a tracing SDK.

**Negative / trade-offs**

- A `-javaagent` and a ~23 MB jar in the image; slightly longer startup.
- Tracing config lives in env/agent, not in Spring config — two places to know about.
- Custom business spans (if wanted later) need the OTel API rather than `@Observed`.

**Revisit when:** the Boot/micrometer-tracing handler-attachment issue is fixed
upstream — the in-process bridge would let us annotate domain methods with
`@Observed` and keep everything in Spring config.

## Alternatives considered

- **In-process micrometer-tracing bridge** (`spring-boot-starter-opentelemetry`) —
  the intended approach; broken on Boot 4.1 as described. Revisit after an upstream fix.
- **Manually attach the tracing handlers** via a custom `ObservationRegistry`
  post-processor — works, but it patches around a framework bug and risks
  double-registration; fragile across Boot upgrades.
- **Zipkin/Brave instead of OTel** — no reason to diverge from OpenTelemetry.
