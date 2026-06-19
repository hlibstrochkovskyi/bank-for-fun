# ADR-0002: Money representation — integer minor units

- **Status:** Accepted
- **Date:** 2026-06-19

## Context

Money is the domain. How we represent monetary amounts in code and in the database
determines whether the ledger can be correct at all. The forces:

- **Binary floating point (`float`/`double`) cannot represent decimal money
  exactly** (e.g. `0.1 + 0.2 != 0.3`). Using it for money is an instant
  credibility loss and a source of real rounding bugs.
- Amounts must round-trip between Java and PostgreSQL without precision loss.
- Some operations (interest, fees) are *rate math* and inherently produce
  fractions that must be rounded deterministically before they touch the ledger.
- We will carry a currency on every amount, even while single-currency, so that
  multi-currency later is not a rewrite.

Two correct storage options exist: **integer minor units** (`BIGINT` cents) or
**`NUMERIC(19,4)` with `BigDecimal`**. Both avoid floating point. We must pick one
and be consistent.

## Decision

We will represent money as **signed integer minor units** (e.g. cents) stored in
`BIGINT`, wrapped in an immutable `Money` value object that always carries a
3-letter currency code (ISO 4217).

- `Money` is immutable, has no public setters, and validates currency match on
  every arithmetic operation (you cannot add USD to EUR).
- Ledger amounts are stored as signed `BIGINT` minor units; debits and credits use
  a sign convention, and every posting's entries sum to exactly zero.
- **Rate math** (interest, fees) is computed in `BigDecimal` with an explicit
  rounding mode of **`HALF_EVEN`** (banker's rounding — the financial standard),
  and only the rounded minor-unit result is posted to the ledger.

## Consequences

**Positive**

- Integer arithmetic over minor units is exact — no rounding error can accumulate
  in stored balances or entries.
- The `Money` value object centralises validation and signals deliberate value-object
  / defensive domain modelling.
- A `BIGINT` comfortably holds amounts far beyond any realistic balance in this
  simulation (~92 quadrillion minor units).

**Negative / trade-offs**

- The application must consistently convert between minor units (storage/transport)
  and human-facing decimal strings (display). The `Money` type owns this.
- Currencies with other-than-2 decimal places (e.g. JPY = 0, some = 3) require the
  minor-unit scale to come from the currency, not a hard-coded `100`. `Money` will
  derive scale from the currency.
- Rate math still needs `BigDecimal` + an explicit rounding mode; minor units do not
  eliminate rounding, they just confine it to where fractions are genuinely created.

**Revisit if:** we needed sub-minor-unit precision in stored balances (we do not for
retail banking), at which point `NUMERIC(19,4)` would be reconsidered.

## Alternatives considered

- **`NUMERIC(19,4)` + `BigDecimal` end to end** — also correct and a fine choice;
  rejected only for consistency and simplicity. Integer minor units make the
  conservation invariant a plain integer sum and remove rounding from the hot path.
- **`float`/`double`** — rejected outright. Cannot represent decimal money exactly.
- **A raw `long` with no wrapper** — rejected. Loses currency safety and invites
  mixing currencies or magnitudes; the `Money` value object is the point.
