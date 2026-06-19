# ADR-0006: Keycloak (OIDC) over hand-rolled authentication

- **Status:** Accepted
- **Date:** 2026-06-19

## Context

The bank needs authentication and authorization: users sign in, and a user may only
touch *their own* accounts. We could hand-roll this (a users table, password
hashing, our own JWT issuing and refresh handling) or delegate identity to a real
identity provider speaking OIDC/OAuth2.

Forces:

- Authentication is **security-critical and easy to get subtly wrong** (password
  storage, token signing, refresh rotation, revocation). Real IdPs have solved this.
- Learning value: OIDC/OAuth2 is the industry standard; using it properly is more
  valuable signal than reinventing it.
- The core bank should stay focused on **money correctness**, not identity plumbing.
- We still want tests to run fast and offline, without a running IdP.

## Decision

Use **Keycloak** as the OIDC identity provider. The core bank is a stateless
**OAuth2 resource server**: it validates the JWT bearer token on each request
against Keycloak's JWK set and trusts no client-supplied identity.

- Keycloak runs as a container in `docker-compose`, with a version-controlled realm
  export (`infra/keycloak/realm-export.json`) so the whole setup is reproducible.
- The token **subject (`sub`)** is the application user id; an account's owner is
  that id. **Resource authorization** is enforced server-side: every account access
  checks ownership against the authenticated subject — the client is never trusted.
- The resource server uses a **`jwk-set-uri`** fetched lazily, so app startup does
  not depend on Keycloak being up.
- Tests authenticate with **mock JWTs** (`spring-security-test`), so the web layer
  and authorization rules are tested without running Keycloak.

## Consequences

**Positive**

- Password storage, token issuance/rotation, and the login UI are handled by a
  hardened, standard provider — less security surface we own.
- The app demonstrates the real OAuth2 resource-server pattern.
- Reproducible via the realm export; tests stay fast and offline.

**Negative / trade-offs**

- Another container to run locally (Keycloak); more moving parts in `docker-compose`.
- A real dependency on an external IdP at runtime for actual logins (mitigated for
  startup by lazy JWKS fetching).
- Some Keycloak-specific configuration to understand (realms, clients).

## Alternatives considered

- **Hand-rolled JWT auth** (`[learning-driven]` in the plan) — more control and one
  fewer container, but we'd own the most security-sensitive code in the system, and
  it shows less than using OIDC correctly. Kept as the documented fallback.
- **Spring Authorization Server** — a real OAuth2 server in-process; lighter than
  Keycloak but less of a "real IdP" experience and still identity plumbing we'd run.
- **Session cookies instead of bearer tokens** — at odds with the stateless API +
  SPA frontend direction.
