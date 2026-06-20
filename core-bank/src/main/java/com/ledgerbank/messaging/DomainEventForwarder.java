package com.ledgerbank.messaging;

import com.ledgerbank.shared.events.TransferHeldEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges in-process domain events to RabbitMQ for async consumers (notifications,
 * future fraud workers). Forwarding happens <em>after commit</em>, so only events
 * for durably-committed changes are published. Publishing failures are logged, not
 * propagated — the business transaction has already succeeded.
 */
@Component
class DomainEventForwarder {

	private static final Logger log = LoggerFactory.getLogger(DomainEventForwarder.class);

	private final RabbitTemplate rabbitTemplate;

	DomainEventForwarder(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void onTransferHeld(TransferHeldEvent event) {
		publish(EventRouting.TRANSFER_HELD, event);
	}

	private void publish(String routingKey, Object event) {
		try {
			rabbitTemplate.convertAndSend(EventRouting.EXCHANGE, routingKey, event);
		}
		catch (RuntimeException e) {
			log.warn("failed to publish event to RabbitMQ (key={}): {}", routingKey, e.getMessage());
		}
	}
}
