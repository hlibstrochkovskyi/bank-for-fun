package com.ledgerbank.notifications;

import com.ledgerbank.messaging.EventRouting;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The notifications worker's durable queue, bound to held-transfer events. */
@Configuration
class NotificationsRabbitConfig {

	static final String HELD_QUEUE = "ledgerbank.notifications.held";

	@Bean
	Queue heldNotificationsQueue() {
		return new Queue(HELD_QUEUE, true);
	}

	@Bean
	Binding heldNotificationsBinding(Queue heldNotificationsQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(heldNotificationsQueue).to(eventsExchange).with(EventRouting.TRANSFER_HELD);
	}
}
