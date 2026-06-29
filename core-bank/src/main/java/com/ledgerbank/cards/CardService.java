package com.ledgerbank.cards;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and serves payment cards. A card is a presentation/identity object bound
 * to an account — it carries no money of its own; spending still posts to the
 * account's ledger. We generate only a last-four and an expiry (no real PAN).
 */
@Service
public class CardService {

	private static final int VALID_YEARS = 4;

	private final CardRepository cards;

	public CardService(CardRepository cards) {
		this.cards = cards;
	}

	@Transactional
	public Card issue(UUID accountId, UUID ownerId, String cardholder) {
		String holder = (cardholder == null || cardholder.isBlank())
				? "Cardholder" : cardholder.trim().toUpperCase();
		CardNetwork network = ThreadLocalRandom.current().nextBoolean()
				? CardNetwork.VISA : CardNetwork.MASTERCARD;
		String last4 = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10_000));
		LocalDate expiry = LocalDate.now().plusYears(VALID_YEARS);
		return cards.save(new Card(accountId, ownerId, holder, network, last4,
				expiry.getMonthValue(), expiry.getYear()));
	}

	@Transactional(readOnly = true)
	public List<Card> listOwnedBy(UUID ownerId) {
		return cards.findByOwnerIdOrderByCreatedAtDesc(ownerId);
	}

	@Transactional(readOnly = true)
	public List<Card> forAccount(UUID accountId) {
		return cards.findByAccountId(accountId);
	}
}
