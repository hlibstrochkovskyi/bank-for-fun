# ADR-0010: A Backend-for-Frontend (BFF) for the web client

- **Status:** Accepted
- **Date:** 2026-06-20

## Context

The Next.js frontend must call the protected core-bank API, which is an OAuth2
resource server expecting a Keycloak JWT bearer token. Two common patterns:

1. **Token in the browser:** the SPA holds the access token (e.g. in memory) and
   calls the resource server directly. Requires CORS on the API and exposes the
   token to any JavaScript running on the page (an XSS becomes a token theft).
2. **Backend-for-Frontend (BFF):** the Next.js server holds the tokens; the browser
   talks only to the Next.js app, which proxies API calls server-side with the token
   attached.

The frontend already has a server (Next.js App Router), so a BFF is natural.

## Decision

Use the **BFF pattern**. Auth.js (NextAuth v5) runs the OIDC code flow with a
**confidential** Keycloak client and stores tokens in an encrypted, httpOnly session
cookie — never exposed to client JavaScript. A catch-all route handler
(`/api/bank/[...path]`) attaches the access token server-side and proxies to
core-bank. Access tokens are refreshed server-side in the Auth.js `jwt` callback.

- The browser only ever calls same-origin `/api/bank/*` — **no CORS** to configure
  on the API, and **no bearer token in the browser**.
- Route protection runs in the Next 16 **proxy** (renamed middleware) via the
  `authorized` callback; unauthenticated app routes redirect to `/login`.
- The `Idempotency-Key` header is forwarded through the proxy unchanged.

## Consequences

**Positive**

- Tokens stay server-side in an httpOnly cookie — an XSS cannot exfiltrate them.
- Same-origin requests; the resource server needs no CORS config for the SPA.
- Token refresh and the OIDC client secret live on the server, where they belong.

**Negative / trade-offs**

- One extra network hop (browser → Next server → core-bank); negligible locally.
- The Next.js server is now on the request path for every API call (it must be up).
- Two "API surfaces" to reason about: the BFF routes and the core-bank API.

## Alternatives considered

- **Token in the browser + CORS** — simpler wiring, but exposes tokens to JS and
  needs CORS; weaker security posture. Rejected.
- **A public SPA client with PKCE, tokens in memory** — avoids a client secret but
  still puts tokens in the browser. Rejected for the same reason.
- **Server Components fetching directly with the session token** — used for some
  reads, but a single proxy keeps client-side TanStack Query (caching, mutations,
  optimistic updates) straightforward and consistent.
