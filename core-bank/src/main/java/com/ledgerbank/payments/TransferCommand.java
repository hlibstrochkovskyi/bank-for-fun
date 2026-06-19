package com.ledgerbank.payments;

import com.ledgerbank.shared.Money;
import java.util.Objects;
import java.util.UUID;

/** Move funds from one account to another. */
public record TransferCommand(
		UUID fromAccountId,
		UUID toAccountId,
		Money amount,
		String idempotencyKey,
		String description) {

	public TransferCommand {
		Objects.requireNonNull(fromAccountId, "fromAccountId must not be null");
		Objects.requireNonNull(toAccountId, "toAccountId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
	}
}
