package com.ledgerbank.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * Derived snapshot of an account's balance — a cache of the ledger, updated in the
 * same transaction as the entries that change it. Carries the account's currency
 * and overdraft floor so the ledger can lock, enforce, and report without reaching
 * into the {@code accounts} module. Implements {@link Persistable} so Spring Data
 * issues an INSERT (not a SELECT-then-merge) for the assigned-id primary key.
 */
@Entity
@Table(name = "account_balance")
public class AccountBalance implements Persistable<UUID> {

	@Id
	@Column(name = "account_id")
	private UUID accountId;

	private String currency;

	private long balance;

	@Column(name = "min_balance")
	private Long minBalance;

	@Version
	private long version;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Transient
	private boolean isNew;

	protected AccountBalance() {
		// for JPA
	}

	/** A fresh zero balance. {@code minBalance == null} means unbounded. */
	public static AccountBalance open(UUID accountId, String currency, Long minBalance) {
		AccountBalance b = new AccountBalance();
		b.accountId = accountId;
		b.currency = currency;
		b.minBalance = minBalance;
		b.balance = 0L;
		b.isNew = true;
		return b;
	}

	/** Apply a signed delta to the snapshot, overflowing loudly. */
	void apply(long delta) {
		this.balance = Math.addExact(this.balance, delta);
	}

	@Override
	public UUID getId() {
		return accountId;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}

	public UUID accountId() {
		return accountId;
	}

	public String currency() {
		return currency;
	}

	public long balance() {
		return balance;
	}

	public Long minBalance() {
		return minBalance;
	}

	public long version() {
		return version;
	}

	public OffsetDateTime updatedAt() {
		return updatedAt;
	}
}
