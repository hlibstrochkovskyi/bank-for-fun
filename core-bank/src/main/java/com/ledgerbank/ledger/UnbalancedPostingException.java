package com.ledgerbank.ledger;

/** Thrown when a posting's legs do not satisfy the double-entry invariant. */
public class UnbalancedPostingException extends RuntimeException {

	public UnbalancedPostingException(String message) {
		super(message);
	}
}
