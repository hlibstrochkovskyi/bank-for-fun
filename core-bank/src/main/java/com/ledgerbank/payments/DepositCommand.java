package com.ledgerbank.payments;

import com.ledgerbank.shared.Money;
import java.util.Objects;
import java.util.UUID;

/** Deposit external funds into a customer account (from the system clearing account). */
public record DepositCommand(UUID accountId, Money amount, String idempotencyKey, String description) {

	public DepositCommand {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
	}
}
