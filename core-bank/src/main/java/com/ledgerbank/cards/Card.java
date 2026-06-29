package com.ledgerbank.cards;

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

/** A payment card bound to a customer account. Stores only the last four digits. */
@Entity
@Table(name = "card")
public class Card {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "account_id")
	private UUID accountId;

	@Column(name = "owner_id")
	private UUID ownerId;

	private String cardholder;

	@Enumerated(EnumType.STRING)
	private CardNetwork network;

	private String last4;

	@Column(name = "exp_month")
	private int expMonth;

	@Column(name = "exp_year")
	private int expYear;

	@Enumerated(EnumType.STRING)
	private CardStatus status;

	@CreationTimestamp
	@Column(name = "created_at")
	private OffsetDateTime createdAt;

	protected Card() {
		// for JPA
	}

	public Card(UUID accountId, UUID ownerId, String cardholder, CardNetwork network, String last4,
			int expMonth, int expYear) {
		this.accountId = accountId;
		this.ownerId = ownerId;
		this.cardholder = cardholder;
		this.network = network;
		this.last4 = last4;
		this.expMonth = expMonth;
		this.expYear = expYear;
		this.status = CardStatus.ACTIVE;
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

	public String cardholder() {
		return cardholder;
	}

	public CardNetwork network() {
		return network;
	}

	public String last4() {
		return last4;
	}

	public int expMonth() {
		return expMonth;
	}

	public int expYear() {
		return expYear;
	}

	public CardStatus status() {
		return status;
	}

	public OffsetDateTime createdAt() {
		return createdAt;
	}
}
