package com.ledgerbank.ledger;

import java.util.List;
import java.util.Objects;

/**
 * A request to record one balanced posting. The {@code legs} must sum to zero per
 * currency; {@code idempotencyKey} is unique across all postings.
 */
public record RecordPostingCommand(
		PostingType type,
		String idempotencyKey,
		String description,
		List<LedgerLeg> legs) {

	public RecordPostingCommand {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
		legs = List.copyOf(legs);
	}
}
