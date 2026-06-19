package com.ledgerbank.web;

import java.util.UUID;

/** Result of a money operation: the posting it produced and the resulting balance. */
public record PaymentResponse(UUID postingId, MoneyView balanceAfter) {
}
