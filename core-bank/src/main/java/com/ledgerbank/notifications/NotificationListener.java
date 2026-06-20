package com.ledgerbank.notifications;

import com.ledgerbank.shared.events.TransferHeldEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consumes domain events from RabbitMQ and turns them into customer notifications. */
@Component
class NotificationListener {

	private final NotificationService notifications;

	NotificationListener(NotificationService notifications) {
		this.notifications = notifications;
	}

	@RabbitListener(queues = NotificationsRabbitConfig.HELD_QUEUE)
	void onTransferHeld(TransferHeldEvent event) {
		notifications.notifyTransferHeld(event);
	}
}
