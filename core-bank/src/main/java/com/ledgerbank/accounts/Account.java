package com.ledgerbank.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A customer-facing or system account. Identity and metadata only — the money
 * (balances) lives in the {@code ledger} module, keyed by this account's id.
 */
@Entity
@Table(name = "account")
public class Account {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "owner_id")
	private UUID ownerId;

	@Enumerated(EnumType.STRING)
	private AccountType type;

	private String currency;

	@Enumerated(EnumType.STRING)
	private AccountStatus status;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected Account() {
		// for JPA
	}

	public Account(UUID ownerId, AccountType type, Currency currency) {
		this.ownerId = ownerId;
		this.type = type;
		this.currency = currency.getCurrencyCode();
		this.status = AccountStatus.ACTIVE;
	}

	public UUID id() {
		return id;
	}

	public UUID ownerId() {
		return ownerId;
	}

	public AccountType type() {
		return type;
	}

	public Currency currency() {
		return Currency.getInstance(currency);
	}

	public AccountStatus status() {
		return status;
	}

	public OffsetDateTime createdAt() {
		return createdAt;
	}
}
