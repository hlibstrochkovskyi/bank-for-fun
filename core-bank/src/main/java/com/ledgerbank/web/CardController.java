package com.ledgerbank.web;

import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.cards.Card;
import com.ledgerbank.cards.CardService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CardController {

	private final CardService cards;
	private final AccountService accounts;

	public CardController(CardService cards, AccountService accounts) {
		this.cards = cards;
		this.accounts = accounts;
	}

	/** Issue a card against an account the caller owns; cardholder taken from the token. */
	@PostMapping("/api/accounts/{accountId}/cards")
	public ResponseEntity<CardResponse> issue(@PathVariable UUID accountId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = Principals.userId(jwt);
		Account account = accounts.requireOwnedBy(accountId, ownerId);
		Card card = cards.issue(accountId, ownerId, cardholderName(jwt));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(CardResponse.of(card, account.nickname()));
	}

	@GetMapping("/api/cards")
	public List<CardResponse> list(@AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = Principals.userId(jwt);
		Map<UUID, String> nicknames = accounts.listOwnedBy(ownerId).stream()
				.collect(Collectors.toMap(Account::id,
						a -> a.nickname() == null ? "" : a.nickname(), (a, b) -> a));
		return cards.listOwnedBy(ownerId).stream()
				.map(card -> CardResponse.of(card, nicknames.get(card.accountId())))
				.toList();
	}

	private static String cardholderName(Jwt jwt) {
		String name = jwt.getClaimAsString("name");
		if (name == null || name.isBlank()) {
			name = jwt.getClaimAsString("preferred_username");
		}
		return name;
	}
}
