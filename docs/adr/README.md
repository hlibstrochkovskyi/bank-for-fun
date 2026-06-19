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

## Planned

These decisions are made as their phase arrives (see the implementation plan):

- 0003 — Spring MVC + virtual threads over WebFlux
- 0004 — Pessimistic locking + deterministic lock ordering
- 0005 — Idempotency strategy
- 0006 — Keycloak over hand-rolled auth
- 0007 — Fraud engine as a separate Python service
- 0008 — RabbitMQ over Kafka
