package com.ledgerbank.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Request to deposit external funds into an account. {@code amount} is major units (e.g. 100.00). */
public record DepositRequest(
		@NotNull @Positive BigDecimal amount,
		@NotBlank String currency,
		String description) {
}
