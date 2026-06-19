package com.ledgerbank.ledger;

import java.util.UUID;

/** Thrown when a posting would push an account below its allowed minimum balance. */
public class InsufficientFundsException extends RuntimeException {

	private final UUID accountId;

	public InsufficientFundsException(UUID accountId, long projectedBalance, long minBalance) {
		super("account %s would drop to %d, below its minimum of %d"
				.formatted(accountId, projectedBalance, minBalance));
		this.accountId = accountId;
	}

	public UUID accountId() {
		return accountId;
	}
}
