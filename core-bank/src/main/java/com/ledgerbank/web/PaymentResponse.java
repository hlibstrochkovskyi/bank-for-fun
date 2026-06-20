package com.ledgerbank.web;

import com.ledgerbank.payments.PaymentResult;
import java.util.UUID;

/**
 * Result of a money operation: COMPLETED with the posting it produced, or HELD with
 * the id of the held transfer awaiting review.
 */
public record PaymentResponse(String status, UUID postingId, UUID heldTransferId, MoneyView balanceAfter) {

	public static PaymentResponse from(PaymentResult result, MoneyView balanceAfter) {
		return new PaymentResponse(result.status().name(), result.postingId(),
				result.heldTransferId(), balanceAfter);
	}
}
