package com.ledgerbank.web;

import com.ledgerbank.payments.HeldTransfer;
import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

/** The admin review view of a held transfer — includes the risk score and reasons. */
public record AdminHeldTransferResponse(
		UUID id,
		UUID fromAccountId,
		UUID toAccountId,
		MoneyView amount,
		double riskScore,
		String reason,
		String status,
		OffsetDateTime createdAt) {

	public static AdminHeldTransferResponse from(HeldTransfer h) {
		return new AdminHeldTransferResponse(h.id(), h.fromAccountId(), h.toAccountId(),
				MoneyView.from(Money.of(h.amount(), Currency.getInstance(h.currency()))),
				h.riskScore(), h.reason(), h.status().name(), h.createdAt());
	}
}
