package com.ledgerbank.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Create a standing order from the path account. {@code firstRunAt} defaults to now. */
public record CreateStandingOrderRequest(
		@NotNull UUID toAccountId,
		@NotNull @Positive BigDecimal amount,
		@NotBlank String currency,
		@Positive int intervalDays,
		OffsetDateTime firstRunAt,
		String description) {
}
