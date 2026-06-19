package com.ledgerbank.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Durable record of a processed money-moving request, keyed by its client-supplied
 * idempotency key. Lets a retried request return the original result instead of
 * processing again. The stored {@code response} is the serialized {@link PaymentResult}.
 */
@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord implements Persistable<String> {

	@Id
	private String key;

	@Column(name = "request_hash")
	private String requestHash;

	@JdbcTypeCode(SqlTypes.JSON)
	private String response;

	@Enumerated(EnumType.STRING)
	private IdempotencyStatus status;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected IdempotencyRecord() {
		// for JPA
	}

	public IdempotencyRecord(String key, String requestHash, String response, IdempotencyStatus status) {
		this.key = key;
		this.requestHash = requestHash;
		this.response = response;
		this.status = status;
	}

	@Override
	public String getId() {
		return key;
	}

	@Override
	public boolean isNew() {
		// Always created fresh via the constructor; we never load-then-save.
		return true;
	}

	public String key() {
		return key;
	}

	public String requestHash() {
		return requestHash;
	}

	public String response() {
		return response;
	}

	public IdempotencyStatus status() {
		return status;
	}
}
