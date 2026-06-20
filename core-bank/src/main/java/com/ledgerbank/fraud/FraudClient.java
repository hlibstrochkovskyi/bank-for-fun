package com.ledgerbank.fraud;

/** Port to the fraud/risk engine. Implementations must never throw — they fail open. */
public interface FraudClient {

	RiskDecision score(FraudCheckRequest request);
}
