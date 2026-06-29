package com.ledgerbank.enrichment;

import com.ledgerbank.enrichment.Categorizer.Enrichment;
import com.ledgerbank.shared.events.MoneyPostedEvent;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists and serves posting enrichment (merchant + category). It listens to the
 * ledger's {@link MoneyPostedEvent} (the published-event API — no direct call into
 * the ledger) and stores a categorization for each posting so spending can be
 * aggregated in SQL. Reads fall back to computing on the fly, so display is correct
 * even for postings recorded before this service existed.
 */
@Service
public class EnrichmentService {

	private final PostingEnrichmentRepository repository;
	private final Categorizer categorizer;

	public EnrichmentService(PostingEnrichmentRepository repository, Categorizer categorizer) {
		this.repository = repository;
		this.categorizer = categorizer;
	}

	/** Runs synchronously within the posting transaction (the publisher is in-process). */
	@EventListener
	@Transactional
	public void onMoneyPosted(MoneyPostedEvent event) {
		if (repository.existsById(event.postingId())) {
			return;
		}
		Enrichment e = categorizer.categorize(event.postingType(), event.description());
		repository.save(new PostingEnrichment(event.postingId(), e.merchant(), e.category()));
	}

	/** Compute enrichment for a posting from its type and description (no persistence). */
	public Enrichment describe(String postingType, String description) {
		return categorizer.categorize(postingType, description);
	}

	/** Stored enrichments for the given postings, keyed by posting id. */
	@Transactional(readOnly = true)
	public Map<UUID, PostingEnrichment> forPostings(Collection<UUID> postingIds) {
		if (postingIds.isEmpty()) {
			return Map.of();
		}
		return repository.findByPostingIdIn(postingIds).stream()
				.collect(Collectors.toMap(PostingEnrichment::postingId, Function.identity()));
	}
}
