package com.ledgerbank.web;

import com.ledgerbank.cards.Card;
import java.util.UUID;

public record CardResponse(
		UUID id,
		UUID accountId,
		String accountNickname,
		String cardholder,
		String network,
		String last4,
		int expMonth,
		int expYear,
		String status) {

	public static CardResponse of(Card card, String accountNickname) {
		return new CardResponse(card.id(), card.accountId(), accountNickname, card.cardholder(),
				card.network().name(), card.last4(), card.expMonth(), card.expYear(), card.status().name());
	}
}
