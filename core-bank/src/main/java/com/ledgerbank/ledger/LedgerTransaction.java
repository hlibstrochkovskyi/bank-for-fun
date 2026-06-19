package com.ledgerbank.ledger;

import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One row of an account's transaction history: a ledger entry enriched with its
 * posting's type and description. {@code amount} is signed (credit positive).
 */
public record LedgerTransaction(
		long entryId,
		UUID postingId,
		PostingType type,
		Money amount,
		String description,
		OffsetDateTime createdAt) {
}
