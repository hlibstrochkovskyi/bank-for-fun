package com.ledgerbank.standingorders;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages standing orders and drives their execution. {@link #runDue()} finds the
 * orders whose time has come and runs each via {@link StandingOrderExecutor} in its
 * own transaction.
 */
@Service
public class StandingOrderService {

	private final StandingOrderRepository orders;
	private final StandingOrderExecutor executor;

	public StandingOrderService(StandingOrderRepository orders, StandingOrderExecutor executor) {
		this.orders = orders;
		this.executor = executor;
	}

	@Transactional
	public StandingOrder create(CreateStandingOrderCommand command) {
		return orders.save(new StandingOrder(
				command.ownerId(), command.fromAccountId(), command.toAccountId(),
				command.amount().minorUnits(), command.amount().currency().getCurrencyCode(),
				command.description(), command.intervalDays(), command.firstRunAt()));
	}

	@Transactional(readOnly = true)
	public List<StandingOrder> listOwnedBy(UUID ownerId) {
		return orders.findByOwnerIdOrderByCreatedAt(ownerId);
	}

	/** Execute every order that is due now. Returns how many were attempted. */
	public int runDue() {
		List<StandingOrder> due = orders.findByStatusAndNextRunAtLessThanEqual(
				StandingOrderStatus.ACTIVE, OffsetDateTime.now(ZoneOffset.UTC));
		due.forEach(order -> executor.execute(order.id()));
		return due.size();
	}
}
