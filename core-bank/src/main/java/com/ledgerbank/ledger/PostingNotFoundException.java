package com.ledgerbank.ledger;

import java.util.UUID;

/** Thrown when a posting cannot be found by id. */
public class PostingNotFoundException extends RuntimeException {

	public PostingNotFoundException(UUID postingId) {
		super("posting not found: " + postingId);
	}
}
