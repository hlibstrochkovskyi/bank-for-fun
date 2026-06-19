package com.ledgerbank.statements;

import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.ledger.LedgerTransaction;
import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derived views over the ledger. A statement is the opening balance (everything
 * before the period), the lines within the period, the closing balance, and the
 * credit/debit totals — all re-derived from the immutable entries.
 */
@Service
public class StatementsService {

	private final LedgerService ledger;

	public StatementsService(LedgerService ledger) {
		this.ledger = ledger;
	}

	@Transactional(readOnly = true)
	public Statement statement(UUID accountId, OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
		Money opening = ledger.balanceAsOf(accountId, fromInclusive);
		Currency currency = opening.currency();
		List<LedgerTransaction> lines = ledger.entriesBetween(accountId, fromInclusive, toExclusive);

		Money credits = Money.zero(currency);
		Money debits = Money.zero(currency);
		Money net = Money.zero(currency);
		for (LedgerTransaction line : lines) {
			net = net.plus(line.amount());
			if (line.amount().isPositive()) {
				credits = credits.plus(line.amount());
			}
			else {
				debits = debits.plus(line.amount().negate());
			}
		}
		Money closing = opening.plus(net);
		return new Statement(accountId, fromInclusive, toExclusive, opening, closing, credits, debits, lines);
	}
}
