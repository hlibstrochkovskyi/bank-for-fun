package com.ledgerbank.payments;

import java.util.UUID;

/**
 * The outcome of a money operation: either COMPLETED with the posting it produced,
 * or HELD with the id of the held transfer awaiting review.
 */
public record PaymentResult(PaymentStatus status, UUID postingId, UUID heldTransferId) {

	public static PaymentResult completed(UUID postingId) {
		return new PaymentResult(PaymentStatus.COMPLETED, postingId, null);
	}

	public static PaymentResult held(UUID heldTransferId) {
		return new PaymentResult(PaymentStatus.HELD, null, heldTransferId);
	}
}
