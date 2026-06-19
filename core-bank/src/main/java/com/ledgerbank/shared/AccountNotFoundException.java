package com.ledgerbank.shared;

import java.util.UUID;

/** Thrown when an account (or its ledger balance) cannot be found by id. */
public class AccountNotFoundException extends RuntimeException {

	private final UUID accountId;

	public AccountNotFoundException(UUID accountId) {
		super("account not found: " + accountId);
		this.accountId = accountId;
	}

	public UUID accountId() {
		return accountId;
	}
}
