package com.ledgerbank.web;

import com.ledgerbank.shared.Money;
import com.ledgerbank.standingorders.StandingOrder;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

public record StandingOrderResponse(
		UUID id,
		UUID fromAccountId,
		UUID toAccountId,
		MoneyView amount,
		int intervalDays,
		OffsetDateTime nextRunAt,
		String status,
		String description) {

	public static StandingOrderResponse from(StandingOrder o) {
		return new StandingOrderResponse(o.id(), o.fromAccountId(), o.toAccountId(),
				MoneyView.from(Money.of(o.amount(), Currency.getInstance(o.currency()))),
				o.intervalDays(), o.nextRunAt(), o.status().name(), o.description());
	}
}
