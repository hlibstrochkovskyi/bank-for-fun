package com.ledgerbank.messaging;

/** RabbitMQ routing constants for ledger-bank domain events. */
public final class EventRouting {

	/** Topic exchange all domain events are published to. */
	public static final String EXCHANGE = "ledgerbank.events";

	/** Routing key for {@code TransferHeldEvent}. */
	public static final String TRANSFER_HELD = "transfer.held";

	private EventRouting() {
	}
}
