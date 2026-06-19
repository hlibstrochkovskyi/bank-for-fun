package com.ledgerbank.standingorders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically runs due standing orders. Disabled in tests (which drive
 * {@link StandingOrderService#runDue()} directly) via the scheduler property.
 */
@Component
@ConditionalOnProperty(name = "ledgerbank.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class StandingOrderScheduler {

	private static final Logger log = LoggerFactory.getLogger(StandingOrderScheduler.class);

	private final StandingOrderService standingOrders;

	StandingOrderScheduler(StandingOrderService standingOrders) {
		this.standingOrders = standingOrders;
	}

	@Scheduled(fixedDelayString = "${ledgerbank.scheduler.standing-orders.fixed-delay:60000}")
	void runDueStandingOrders() {
		int attempted = standingOrders.runDue();
		if (attempted > 0) {
			log.info("Executed {} due standing order(s)", attempted);
		}
	}
}
