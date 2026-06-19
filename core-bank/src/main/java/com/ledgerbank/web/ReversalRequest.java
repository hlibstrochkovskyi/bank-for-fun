package com.ledgerbank.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Admin request to reverse a posting. */
public record ReversalRequest(@NotNull UUID postingId, String reason) {
}
