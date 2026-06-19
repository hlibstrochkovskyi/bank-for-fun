package com.ledgerbank.ledger;

import com.ledgerbank.shared.Money;
import java.util.Objects;
import java.util.UUID;

/** One signed leg of a posting: a {@link Money} amount applied to an account. */
public record LedgerLeg(UUID accountId, Money amount) {

	public LedgerLeg {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
	}
}
