package com.ledgerbank.payments;

import java.util.UUID;

/** The outcome of a money operation: the id of the posting it produced. */
public record PaymentResult(UUID postingId) {
}
