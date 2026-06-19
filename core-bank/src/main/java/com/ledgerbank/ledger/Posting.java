package com.ledgerbank.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A posting groups balanced ledger entries into one atomic financial event.
 * Immutable once written: never updated or deleted.
 */
@Entity
@Table(name = "posting")
public class Posting {

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	private PostingType type;

	@Column(name = "idempotency_key")
	private String idempotencyKey;

	private String description;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected Posting() {
		// for JPA
	}

	public Posting(PostingType type, String idempotencyKey, String description) {
		this.type = type;
		this.idempotencyKey = idempotencyKey;
		this.description = description;
	}

	public UUID id() {
		return id;
	}

	public PostingType type() {
		return type;
	}

	public String idempotencyKey() {
		return idempotencyKey;
	}

	public String description() {
		return description;
	}

	public OffsetDateTime createdAt() {
		return createdAt;
	}
}
