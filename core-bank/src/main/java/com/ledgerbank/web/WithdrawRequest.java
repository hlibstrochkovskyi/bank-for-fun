package com.ledgerbank.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Request to withdraw funds from an account. {@code amount} is major units (e.g. 40.00). */
public record WithdrawRequest(
		@NotNull @Positive BigDecimal amount,
		@NotBlank String currency,
		String description) {
}
