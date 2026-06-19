package com.ledgerbank.web;

import com.ledgerbank.accounts.Account;
import com.ledgerbank.shared.Money;
import java.util.UUID;

public record AccountResponse(UUID id, String type, String currency, String status, MoneyView balance) {

	public static AccountResponse of(Account account, Money balance) {
		return new AccountResponse(account.id(), account.type().name(),
				account.currency().getCurrencyCode(), account.status().name(), MoneyView.from(balance));
	}
}
