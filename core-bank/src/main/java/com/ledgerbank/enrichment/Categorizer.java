package com.ledgerbank.enrichment;

import org.springframework.stereotype.Component;

/**
 * Deterministic, rule-based categorizer: maps a posting's type and free-text
 * description to a display merchant and a spending category. Pure (no I/O), so it
 * can run both at posting time (to persist) and at read time (as a fallback).
 *
 * <p>Descriptions follow a loose "Label — Merchant" convention from the seed and
 * UI (e.g. {@code "Salary — Atlas Studio"}); the merchant is the part after the
 * dash when present, otherwise the whole description.
 */
@Component
public class Categorizer {

	/** A derived merchant name and category for a posting. */
	public record Enrichment(String merchant, TransactionCategory category) {
	}

	public Enrichment categorize(String postingType, String description) {
		String merchant = merchantOf(description);
		String haystack = description == null ? "" : description.toLowerCase();
		return new Enrichment(merchant, categoryOf(postingType, haystack));
	}

	private static String merchantOf(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		int dash = description.lastIndexOf('—');
		String merchant = dash >= 0 ? description.substring(dash + 1) : description;
		return merchant.trim();
	}

	private static TransactionCategory categoryOf(String type, String d) {
		if (d.contains("refund")) {
			return TransactionCategory.SHOPPING;
		}
		if (d.contains("salary") || d.contains("payroll")) {
			return TransactionCategory.INCOME;
		}
		if (d.contains("rent") || d.contains("mortgage") || d.contains("property")) {
			return TransactionCategory.HOUSING;
		}
		if (d.contains("grocer") || d.contains("whole foods") || d.contains("trader")
				|| d.contains("market") || d.contains("aldi") || d.contains("safeway")) {
			return TransactionCategory.GROCERIES;
		}
		if (d.contains("coffee") || d.contains("cafe") || d.contains("restaurant")
				|| d.contains("dining") || d.contains("bar") || d.contains("bottle")
				|| d.contains("chipotle") || d.contains("mcdonald")) {
			return TransactionCategory.DINING;
		}
		if (d.contains("spotify") || d.contains("netflix") || d.contains("hulu")
				|| d.contains("subscription") || d.contains("prime") || d.contains("youtube")) {
			return TransactionCategory.SUBSCRIPTIONS;
		}
		if (d.contains("transit") || d.contains("metro") || d.contains("uber") || d.contains("lyft")
				|| d.contains("fuel") || d.contains("gas") || d.contains("shell")) {
			return TransactionCategory.TRANSPORT;
		}
		if (d.contains("edison") || d.contains("electric") || d.contains("water")
				|| d.contains("utility") || d.contains("comcast") || d.contains("verizon")) {
			return TransactionCategory.UTILITIES;
		}
		if (d.contains("transfer") || d.contains("round-up") || d.contains("roundup")
				|| "TRANSFER".equals(type) || "REVERSAL".equals(type)) {
			return TransactionCategory.TRANSFER;
		}
		if ("DEPOSIT".equals(type)) {
			return TransactionCategory.INCOME;
		}
		if ("WITHDRAWAL".equals(type)) {
			return TransactionCategory.SHOPPING;
		}
		return TransactionCategory.UNCATEGORIZED;
	}
}
