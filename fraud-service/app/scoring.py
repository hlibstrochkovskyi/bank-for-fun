"""Rule-based transaction risk scoring.

Stateless: the caller supplies the signals (amount, whether the payee is new, recent
velocity). Rules contribute to a 0..1 score; at or above HOLD_THRESHOLD the
transaction is held for review. This is intentionally simple and explainable — a
trained anomaly model (e.g. isolation forest) can replace/augment it later.
"""

from dataclasses import dataclass

# Amount thresholds in minor units (cents).
HIGH_AMOUNT_THRESHOLD_MINOR = 1_000_000  # $10,000
MID_AMOUNT_THRESHOLD_MINOR = 100_000     # $1,000

# Transfers in the recent window above this count look like velocity abuse.
VELOCITY_THRESHOLD = 10

# Score at or above which a transaction is held.
HOLD_THRESHOLD = 0.7


@dataclass(frozen=True)
class RiskDecision:
    score: float
    action: str  # "ALLOW" or "HOLD"
    reasons: list[str]


def score_transaction(
    amount_minor: int,
    currency: str,
    new_payee: bool = False,
    recent_transfer_count: int = 0,
) -> RiskDecision:
    score = 0.0
    reasons: list[str] = []

    if amount_minor >= HIGH_AMOUNT_THRESHOLD_MINOR:
        score += 0.8
        reasons.append("amount exceeds the high-value threshold")

    if new_payee and amount_minor >= MID_AMOUNT_THRESHOLD_MINOR:
        score += 0.3
        reasons.append("new payee for a mid-size or larger amount")

    if recent_transfer_count >= VELOCITY_THRESHOLD:
        score += 0.7
        reasons.append("high transfer velocity in the recent window")

    score = min(1.0, score)
    action = "HOLD" if score >= HOLD_THRESHOLD else "ALLOW"
    return RiskDecision(score=score, action=action, reasons=reasons)
