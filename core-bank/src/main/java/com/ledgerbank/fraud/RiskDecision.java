package com.ledgerbank.fraud;

import java.util.List;

/** The fraud engine's risk decision: a 0..1 score, an action, and human-readable reasons. */
public record RiskDecision(double score, FraudAction action, List<String> reasons) {

	public static RiskDecision allow() {
		return new RiskDecision(0.0, FraudAction.ALLOW, List.of());
	}

	public boolean isHold() {
		return action == FraudAction.HOLD;
	}

	public String reasonSummary() {
		return reasons.isEmpty() ? "no reasons" : String.join("; ", reasons);
	}
}
