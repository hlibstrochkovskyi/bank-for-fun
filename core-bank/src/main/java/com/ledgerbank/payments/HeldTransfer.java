package com.ledgerbank.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** A transfer the fraud engine flagged, awaiting review. Until released it is not posted. */
@Entity
@Table(name = "held_transfer")
public class HeldTransfer {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "owner_id")
	private UUID ownerId;

	@Column(name = "from_account_id")
	private UUID fromAccountId;

	@Column(name = "to_account_id")
	private UUID toAccountId;

	private long amount;

	private String currency;

	@Column(name = "idempotency_key")
	private String idempotencyKey;

	@Column(name = "risk_score")
	private double riskScore;

	private String reason;

	private String description;

	@Enumerated(EnumType.STRING)
	private HeldTransferStatus status;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	@Column(name = "reviewed_at")
	private OffsetDateTime reviewedAt;

	protected HeldTransfer() {
		// for JPA
	}

	public HeldTransfer(UUID ownerId, UUID fromAccountId, UUID toAccountId, long amount, String currency,
			String idempotencyKey, double riskScore, String reason, String description) {
		this.ownerId = ownerId;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
		this.currency = currency;
		this.idempotencyKey = idempotencyKey;
		this.riskScore = riskScore;
		this.reason = reason;
		this.description = description;
		this.status = HeldTransferStatus.PENDING_REVIEW;
	}

	void markReleased() {
		this.status = HeldTransferStatus.RELEASED;
		this.reviewedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	void markRejected() {
		this.status = HeldTransferStatus.REJECTED;
		this.reviewedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public UUID id() {
		return id;
	}

	public UUID ownerId() {
		return ownerId;
	}

	public UUID fromAccountId() {
		return fromAccountId;
	}

	public UUID toAccountId() {
		return toAccountId;
	}

	public long amount() {
		return amount;
	}

	public String currency() {
		return currency;
	}

	public String idempotencyKey() {
		return idempotencyKey;
	}

	public double riskScore() {
		return riskScore;
	}

	public String reason() {
		return reason;
	}

	public String description() {
		return description;
	}

	public HeldTransferStatus status() {
		return status;
	}

	public OffsetDateTime createdAt() {
		return createdAt;
	}
}
