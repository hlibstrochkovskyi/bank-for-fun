package com.ledgerbank.payments;

import java.util.Objects;
import java.util.UUID;

/** Reverse a previously recorded posting (a refund/correction). */
public record ReverseCommand(UUID postingId, String idempotencyKey, String reason) {

	public ReverseCommand {
		Objects.requireNonNull(postingId, "postingId must not be null");
		Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
	}
}
