package com.ledgerbank.payments;

public enum PaymentStatus {
	/** The money moved: a posting was recorded. */
	COMPLETED,
	/** The fraud engine flagged it; it is held for review and has not moved. */
	HELD
}
