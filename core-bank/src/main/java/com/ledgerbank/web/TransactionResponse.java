package com.ledgerbank.web;

import com.ledgerbank.ledger.LedgerTransaction;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
		long entryId,
		UUID postingId,
		String type,
		MoneyView amount,
		String description,
		OffsetDateTime createdAt) {

	public static TransactionResponse from(LedgerTransaction tx) {
		return new TransactionResponse(tx.entryId(), tx.postingId(), tx.type().name(),
				MoneyView.from(tx.amount()), tx.description(), tx.createdAt());
	}
}
