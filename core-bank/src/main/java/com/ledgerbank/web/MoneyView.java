package com.ledgerbank.web;

import com.ledgerbank.shared.Money;

/**
 * API representation of a monetary amount: the human-readable major-unit string,
 * the currency, and the exact integer minor units (so clients never have to parse
 * decimals to get exact values).
 */
public record MoneyView(String amount, String currency, long minorUnits) {

	public static MoneyView from(Money money) {
		return new MoneyView(money.toBigDecimal().toPlainString(),
				money.currency().getCurrencyCode(), money.minorUnits());
	}
}
