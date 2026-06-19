package com.ledgerbank.ledger;

import java.util.UUID;

/** Thrown when a posting cannot be reversed (already reversed, or is itself a reversal). */
public class ReversalNotAllowedException extends RuntimeException {

	public ReversalNotAllowedException(UUID postingId, String reason) {
		super("cannot reverse posting %s: %s".formatted(postingId, reason));
	}
}
