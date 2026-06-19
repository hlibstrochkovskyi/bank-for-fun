package com.ledgerbank.web;

import jakarta.validation.constraints.NotBlank;

/** Request to open a customer account. {@code type} is CHECKING or SAVINGS. */
public record OpenAccountRequest(@NotBlank String type, @NotBlank String currency) {
}
