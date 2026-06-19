package com.ledgerbank.accounts;

/**
 * Kinds of account. {@code SYSTEM_*} accounts model the outside world (cash
 * entering/leaving the bank) and may go negative; customer accounts may not.
 */
public enum AccountType {
	CHECKING,
	SAVINGS,
	SYSTEM_CLEARING,
	SYSTEM_EQUITY;

	public boolean isSystem() {
		return name().startsWith("SYSTEM_");
	}
}
