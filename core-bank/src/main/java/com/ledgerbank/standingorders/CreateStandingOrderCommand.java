package com.ledgerbank.standingorders;

import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Create a recurring transfer from {@code fromAccountId} to {@code toAccountId}. */
public record CreateStandingOrderCommand(
		UUID ownerId,
		UUID fromAccountId,
		UUID toAccountId,
		Money amount,
		int intervalDays,
		OffsetDateTime firstRunAt,
		String description) {

	public CreateStandingOrderCommand {
		Objects.requireNonNull(ownerId, "ownerId must not be null");
		Objects.requireNonNull(fromAccountId, "fromAccountId must not be null");
		Objects.requireNonNull(toAccountId, "toAccountId must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(firstRunAt, "firstRunAt must not be null");
		if (intervalDays <= 0) {
			throw new IllegalArgumentException("intervalDays must be positive");
		}
		if (!amount.isPositive()) {
			throw new IllegalArgumentException("amount must be positive");
		}
		if (fromAccountId.equals(toAccountId)) {
			throw new IllegalArgumentException("cannot schedule a transfer to the same account");
		}
	}
}
