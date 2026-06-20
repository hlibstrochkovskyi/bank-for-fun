"""FastAPI entrypoint for the fraud/risk scoring service."""

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.scoring import score_transaction

app = FastAPI(title="ledger-bank fraud service", version="0.1.0")


class ScoreRequest(BaseModel):
    amount_minor: int = Field(ge=0, description="amount in minor units (cents)")
    currency: str
    from_account: str
    to_account: str
    new_payee: bool = False
    recent_transfer_count: int = Field(default=0, ge=0)


class ScoreResponse(BaseModel):
    score: float
    action: str
    reasons: list[str]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/score", response_model=ScoreResponse)
def score(request: ScoreRequest) -> ScoreResponse:
    decision = score_transaction(
        amount_minor=request.amount_minor,
        currency=request.currency,
        new_payee=request.new_payee,
        recent_transfer_count=request.recent_transfer_count,
    )
    return ScoreResponse(score=decision.score, action=decision.action, reasons=decision.reasons)
