package com.ledgerbank.audit;

import com.ledgerbank.shared.events.AccountOpenedEvent;
import com.ledgerbank.shared.events.MoneyPostedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns domain events into audit entries. Listeners run synchronously within the
 * publishing transaction, so an audit row commits if and only if the change it
 * records does — no silent gaps.
 */
@Component
class AuditEventListener {

	private final AuditService audit;

	AuditEventListener(AuditService audit) {
		this.audit = audit;
	}

	@EventListener
	void on(AccountOpenedEvent event) {
		audit.log("ACCOUNT_OPENED", "ACCOUNT", event.accountId().toString(),
				Map.of("type", event.accountType(), "currency", event.currency()));
	}

	@EventListener
	void on(MoneyPostedEvent event) {
		audit.log("MONEY_POSTED", "POSTING", event.postingId().toString(),
				Map.of("type", event.postingType()));
	}
}
