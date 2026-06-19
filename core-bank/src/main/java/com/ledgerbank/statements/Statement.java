package com.ledgerbank.statements;

import com.ledgerbank.ledger.LedgerTransaction;
import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** A date-range statement: opening/closing balances, totals, and the lines between. */
public record Statement(
		UUID accountId,
		OffsetDateTime from,
		OffsetDateTime to,
		Money openingBalance,
		Money closingBalance,
		Money totalCredits,
		Money totalDebits,
		List<LedgerTransaction> transactions) {
}
