package com.ledgerbank.enrichment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Derived display metadata for a posting (merchant + category). See {@link Categorizer}. */
@Entity
@Table(name = "posting_enrichment")
public class PostingEnrichment {

	@Id
	@Column(name = "posting_id")
	private UUID postingId;

	private String merchant;

	@Enumerated(EnumType.STRING)
	private TransactionCategory category;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected PostingEnrichment() {
		// for JPA
	}

	public PostingEnrichment(UUID postingId, String merchant, TransactionCategory category) {
		this.postingId = postingId;
		this.merchant = merchant;
		this.category = category;
	}

	public UUID postingId() {
		return postingId;
	}

	public String merchant() {
		return merchant;
	}

	public TransactionCategory category() {
		return category;
	}
}
