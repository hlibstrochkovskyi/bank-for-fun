package com.ledgerbank.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * The single source of truth for money: one immutable, signed entry against an
 * account within a posting. Debits are negative, credits positive (minor units).
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "posting_id")
	private UUID postingId;

	@Column(name = "account_id")
	private UUID accountId;

	private long amount;

	private String currency;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected LedgerEntry() {
		// for JPA
	}

	public LedgerEntry(UUID postingId, UUID accountId, long amount, String currency) {
		this.postingId = postingId;
		this.accountId = accountId;
		this.amount = amount;
		this.currency = currency;
	}

	public Long id() {
		return id;
	}

	public UUID postingId() {
		return postingId;
	}

	public UUID accountId() {
		return accountId;
	}

	public long amount() {
		return amount;
	}

	public String currency() {
		return currency;
	}

	public OffsetDateTime createdAt() {
		return createdAt;
	}
}
