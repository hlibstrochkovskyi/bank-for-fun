package com.ledgerbank.payments;

/** Thrown when an idempotency key is reused with a different request payload. */
public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException(String key) {
		super("idempotency key reused with a different request: " + key);
	}
}
