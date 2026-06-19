package com.ledgerbank.shared.events;

import java.util.UUID;

/** A balanced posting was recorded. Published by the ledger, consumed by audit. */
public record MoneyPostedEvent(UUID postingId, String postingType, String description) {
}
