package com.ledgerbank.web;

import com.ledgerbank.enrichment.Categorizer.Enrichment;
import com.ledgerbank.enrichment.EnrichmentService;
import com.ledgerbank.enrichment.PostingEnrichment;
import com.ledgerbank.ledger.LedgerTransaction;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TransactionResponse(
		long entryId,
		UUID postingId,
		String type,
		MoneyView amount,
		String description,
		String merchant,
		String category,
		OffsetDateTime createdAt) {

	/**
	 * Enrich a history slice for display. Uses each posting's stored enrichment when
	 * present, falling back to computing it on the fly (so postings recorded before
	 * enrichment existed still display a merchant and category).
	 */
	public static List<TransactionResponse> list(List<LedgerTransaction> txs, EnrichmentService enrichment) {
		Map<UUID, PostingEnrichment> stored = enrichment.forPostings(
				txs.stream().map(LedgerTransaction::postingId).distinct().toList());
		return txs.stream().map(tx -> {
			PostingEnrichment e = stored.get(tx.postingId());
			if (e != null) {
				return of(tx, e.merchant(), e.category().name());
			}
			Enrichment computed = enrichment.describe(tx.type().name(), tx.description());
			return of(tx, computed.merchant(), computed.category().name());
		}).toList();
	}

	private static TransactionResponse of(LedgerTransaction tx, String merchant, String category) {
		return new TransactionResponse(tx.entryId(), tx.postingId(), tx.type().name(),
				MoneyView.from(tx.amount()), tx.description(), merchant, category, tx.createdAt());
	}
}
