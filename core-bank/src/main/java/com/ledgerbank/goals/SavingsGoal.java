package com.ledgerbank.goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A savings goal bound to a savings account. Stores only a name and a target —
 * progress is derived from the account's ledger balance, never stored here.
 */
@Entity
@Table(name = "savings_goal")
public class SavingsGoal {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "account_id")
	private UUID accountId;

	@Column(name = "owner_id")
	private UUID ownerId;

	private String name;

	@Column(name = "target_minor")
	private long targetMinor;

	private String currency;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected SavingsGoal() {
		// for JPA
	}

	public SavingsGoal(UUID accountId, UUID ownerId, String name, long targetMinor, String currency) {
		this.accountId = accountId;
		this.ownerId = ownerId;
		this.name = name;
		this.targetMinor = targetMinor;
		this.currency = currency;
	}

	public void update(String name, long targetMinor) {
		this.name = name;
		this.targetMinor = targetMinor;
	}

	public UUID id() {
		return id;
	}

	public UUID accountId() {
		return accountId;
	}

	public UUID ownerId() {
		return ownerId;
	}

	public String name() {
		return name;
	}

	public long targetMinor() {
		return targetMinor;
	}

	public String currency() {
		return currency;
	}
}
