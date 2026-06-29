package com.ledgerbank.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Request to set a savings goal on an account. {@code target} is major units. */
public record SetGoalRequest(@NotBlank String name, @NotNull @Positive BigDecimal target) {
}
