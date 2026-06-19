package com.ledgerbank.payments;

import com.ledgerbank.shared.Money;
import java.util.Objects;
import java.util.UUID;

/** Withdraw funds from a customer account to the outside world (system clearing account). */
public record WithdrawCommand(UUID accountId, Money amount, String idempotencyKey, String description) {

	public WithdrawCommand {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
	}
}
