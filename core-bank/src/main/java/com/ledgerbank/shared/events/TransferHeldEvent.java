package com.ledgerbank.shared.events;

import java.util.UUID;

/** A transfer was flagged by the fraud engine and held for review. */
public record TransferHeldEvent(
		UUID heldTransferId,
		UUID ownerId,
		UUID fromAccountId,
		UUID toAccountId,
		long amount,
		String currency,
		double riskScore,
		String reason) {
}
