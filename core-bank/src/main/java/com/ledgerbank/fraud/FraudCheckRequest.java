package com.ledgerbank.fraud;

import java.util.UUID;

/** Signals sent to the fraud engine for scoring a money movement. */
public record FraudCheckRequest(
		long amountMinor,
		String currency,
		UUID fromAccount,
		UUID toAccount,
		boolean newPayee,
		int recentTransferCount) {
}
