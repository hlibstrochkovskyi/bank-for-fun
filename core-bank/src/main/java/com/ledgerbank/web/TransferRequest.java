package com.ledgerbank.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/** Request to move funds between two accounts. {@code amount} is major units (e.g. 25.00). */
public record TransferRequest(
		@NotNull UUID fromAccountId,
		@NotNull UUID toAccountId,
		@NotNull @Positive BigDecimal amount,
		@NotBlank String currency,
		String description) {
}
