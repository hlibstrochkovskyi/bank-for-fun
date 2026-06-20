package com.ledgerbank.payments;

import java.util.UUID;

public class HeldTransferNotFoundException extends RuntimeException {

	public HeldTransferNotFoundException(UUID id) {
		super("held transfer not found: " + id);
	}
}
