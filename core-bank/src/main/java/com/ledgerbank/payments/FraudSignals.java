package com.ledgerbank.payments;

import com.ledgerbank.ledger.LedgerEntryRepository;
import com.ledgerbank.ledger.PostingRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Derives the behavioural signals the fraud engine scores (velocity, new payee). */
@Component
class FraudSignals {

	private static final int VELOCITY_WINDOW_HOURS = 1;

	private final LedgerEntryRepository entries;
	private final PostingRepository postings;

	FraudSignals(LedgerEntryRepository entries, PostingRepository postings) {
		this.entries = entries;
		this.postings = postings;
	}

	@Transactional(readOnly = true)
	TransferSignals forTransfer(UUID fromAccountId, UUID toAccountId) {
		OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(VELOCITY_WINDOW_HOURS);
		int recent = (int) entries.countByAccountIdAndAmountLessThanAndCreatedAtAfter(fromAccountId, 0L, since);
		boolean newPayee = !postings.existsTransferBetween(fromAccountId, toAccountId);
		return new TransferSignals(newPayee, recent);
	}

	record TransferSignals(boolean newPayee, int recentTransferCount) {
	}
}
