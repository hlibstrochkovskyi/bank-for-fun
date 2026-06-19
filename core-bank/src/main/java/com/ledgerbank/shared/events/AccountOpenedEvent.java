package com.ledgerbank.shared.events;

import java.util.UUID;

/** A new account was opened. Published by the accounts module, consumed by audit. */
public record AccountOpenedEvent(UUID accountId, UUID ownerId, String accountType, String currency) {
}
