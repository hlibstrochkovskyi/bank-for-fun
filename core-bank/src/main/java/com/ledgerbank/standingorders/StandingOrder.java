package com.ledgerbank.standingorders;

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

/** A recurring transfer scheduled by a customer. */
@Entity
@Table(name = "standing_order")
public class StandingOrder {

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

	private String description;

	@Column(name = "interval_days")
	private int intervalDays;

	@Column(name = "next_run_at")
	private OffsetDateTime nextRunAt;

	@Enumerated(EnumType.STRING)
	private StandingOrderStatus status;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected StandingOrder() {
		// for JPA
	}

	public StandingOrder(UUID ownerId, UUID fromAccountId, UUID toAccountId, long amount,
			String currency, String description, int intervalDays, OffsetDateTime firstRunAt) {
		this.ownerId = ownerId;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount = amount;
		this.currency = currency;
		this.description = description;
		this.intervalDays = intervalDays;
		this.nextRunAt = firstRunAt;
		this.status = StandingOrderStatus.ACTIVE;
	}

	/** Advance to the next occurrence after a run. */
	void advance() {
		this.nextRunAt = this.nextRunAt.plusDays(intervalDays);
	}

	void cancel() {
		this.status = StandingOrderStatus.CANCELLED;
	}

	public boolean isDue(OffsetDateTime now) {
		return status == StandingOrderStatus.ACTIVE && !nextRunAt.isAfter(now);
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

	public String description() {
		return description;
	}

	public int intervalDays() {
		return intervalDays;
	}

	public OffsetDateTime nextRunAt() {
		return nextRunAt;
	}

	public StandingOrderStatus status() {
		return status;
	}
}
