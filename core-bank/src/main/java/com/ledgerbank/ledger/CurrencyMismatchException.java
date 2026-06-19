package com.ledgerbank.ledger;

import java.util.UUID;

/** Thrown when a leg's currency does not match its account's currency. */
public class CurrencyMismatchException extends RuntimeException {

	public CurrencyMismatchException(UUID accountId, String accountCurrency, String legCurrency) {
		super("account %s is %s but leg is %s".formatted(accountId, accountCurrency, legCurrency));
	}
}
