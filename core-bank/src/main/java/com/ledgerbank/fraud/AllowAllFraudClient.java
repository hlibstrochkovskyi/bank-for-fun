package com.ledgerbank.fraud;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default fraud client used when the integration is disabled: allows everything.
 * Keeps the transfer path working (and tests fast) without the Python service.
 */
@Component
@ConditionalOnProperty(name = "ledgerbank.fraud.enabled", havingValue = "false", matchIfMissing = true)
class AllowAllFraudClient implements FraudClient {

	@Override
	public RiskDecision score(FraudCheckRequest request) {
		return RiskDecision.allow();
	}
}
