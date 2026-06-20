# Architecture Decision Records

Each ADR is a short, immutable record of one significant decision: its context,
the decision itself, and the consequences. We write them *as we build*, because
the reasoning is the most valuable thing to capture and the hardest to reconstruct
later. Superseded ADRs are kept and marked, never deleted.

Format: [Michael Nygard's ADR template](https://github.com/joelparkerhenderson/architecture-decision-record).

| #    | Title                                              | Status   |
|------|----------------------------------------------------|----------|
| 0001 | [Modular monolith over microservices](0001-modular-monolith.md) | Accepted |
| 0002 | [Money representation: integer minor units](0002-money-representation.md) | Accepted |
| 0003 | [Spring MVC + virtual threads over WebFlux](0003-mvc-virtual-threads.md) | Accepted |
| 0004 | [Pessimistic locking + deterministic lock ordering](0004-pessimistic-locking.md) | Accepted |
| 0005 | [Idempotency for money-moving operations](0005-idempotency-strategy.md) | Accepted |
| 0006 | [Keycloak (OIDC) over hand-rolled auth](0006-keycloak-auth.md) | Accepted |
| 0007 | [Fraud engine as a separate Python service](0007-fraud-service-python.md) | Accepted |
| 0009 | [Distributed tracing via the OpenTelemetry Java agent](0009-tracing-agent.md) | Accepted |

## Planned

These decisions are made as their phase arrives (see the implementation plan):

- 0008 — RabbitMQ over Kafka
