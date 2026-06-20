package com.ledgerbank.fraud;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the Python fraud service over REST. Fails <em>open</em>: if the service is
 * unavailable or errors, the transaction is allowed (availability over strictness
 * for this simulation; a real bank might fail closed).
 */
@Component
@ConditionalOnProperty(name = "ledgerbank.fraud.enabled", havingValue = "true")
class RestFraudClient implements FraudClient {

	private static final Logger log = LoggerFactory.getLogger(RestFraudClient.class);

	private final RestClient restClient;

	RestFraudClient(FraudProperties properties) {
		this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
	}

	@Override
	public RiskDecision score(FraudCheckRequest request) {
		try {
			ScoreResponse response = restClient.post()
					.uri("/score")
					.body(Map.of(
							"amount_minor", request.amountMinor(),
							"currency", request.currency(),
							"from_account", request.fromAccount().toString(),
							"to_account", request.toAccount().toString(),
							"new_payee", request.newPayee(),
							"recent_transfer_count", request.recentTransferCount()))
					.retrieve()
					.body(ScoreResponse.class);
			if (response == null) {
				return RiskDecision.allow();
			}
			return new RiskDecision(response.score(), FraudAction.valueOf(response.action()),
					response.reasons() != null ? response.reasons() : List.of());
		}
		catch (RuntimeException e) {
			log.warn("fraud service call failed, allowing transaction (fail-open): {}", e.getMessage());
			return RiskDecision.allow();
		}
	}

	private record ScoreResponse(double score, String action, List<String> reasons) {
	}
}
