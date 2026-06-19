package com.ledgerbank.standingorders;

import com.ledgerbank.audit.AuditService;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.TransferCommand;
import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes a single standing order in its own transaction (so one failing order
 * does not affect the others). The order row is locked, re-checked for being due,
 * the transfer is attempted idempotently, and {@code next_run_at} advances by the
 * interval. A funds pre-check under the lock keeps the insufficient-funds case from
 * rolling back the transaction — it is recorded and skipped, and the schedule still
 * advances.
 */
@Component
public class StandingOrderExecutor {

	private final StandingOrderRepository orders;
	private final PaymentsService payments;
	private final LedgerService ledger;
	private final AuditService audit;

	public StandingOrderExecutor(StandingOrderRepository orders, PaymentsService payments,
			LedgerService ledger, AuditService audit) {
		this.orders = orders;
		this.payments = payments;
		this.ledger = ledger;
		this.audit = audit;
	}

	@Transactional
	public void execute(UUID orderId) {
		StandingOrder order = orders.findByIdForUpdate(orderId).orElse(null);
		if (order == null || !order.isDue(OffsetDateTime.now(ZoneOffset.UTC))) {
			return;
		}
		Money amount = Money.of(order.amount(), Currency.getInstance(order.currency()));
		String idempotencyKey = "standing-order:" + order.id() + ":"
				+ order.nextRunAt().toInstant().toEpochMilli();

		if (ledger.balanceOf(order.fromAccountId()).isGreaterThanOrEqualTo(amount)) {
			payments.transfer(new TransferCommand(order.fromAccountId(), order.toAccountId(),
					amount, idempotencyKey, order.description()));
			audit.log("STANDING_ORDER_EXECUTED", "STANDING_ORDER", order.id().toString(),
					Map.of("amount", order.amount(), "currency", order.currency()));
		}
		else {
			audit.log("STANDING_ORDER_SKIPPED", "STANDING_ORDER", order.id().toString(),
					Map.of("reason", "insufficient_funds"));
		}
		order.advance();
	}
}
