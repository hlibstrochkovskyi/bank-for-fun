package com.ledgerbank.shared;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * An immutable monetary amount, stored as signed integer <em>minor units</em>
 * (e.g. cents) together with its {@link Currency}. See ADR-0002.
 *
 * <p>Money is never represented with {@code float}/{@code double}. Arithmetic is
 * exact integer arithmetic that overflows loudly ({@link ArithmeticException})
 * rather than silently wrapping, and every operation validates that both operands
 * share the same currency — you cannot add USD to EUR.
 */
public final class Money implements Comparable<Money> {

	private final long minorUnits;
	private final Currency currency;

	private Money(long minorUnits, Currency currency) {
		this.minorUnits = minorUnits;
		this.currency = Objects.requireNonNull(currency, "currency must not be null");
	}

	/** A {@code Money} of the given signed minor units (e.g. cents) in {@code currency}. */
	public static Money of(long minorUnits, Currency currency) {
		return new Money(minorUnits, currency);
	}

	/** Zero in the given currency. */
	public static Money zero(Currency currency) {
		return new Money(0L, currency);
	}

	/**
	 * Build from a major-unit decimal (e.g. {@code 12.34} USD). The decimal must
	 * not carry more precision than the currency allows; otherwise an
	 * {@link ArithmeticException} is thrown rather than silently rounding — input
	 * money is exact (rate math that genuinely rounds lives elsewhere, see ADR-0002).
	 */
	public static Money ofMajor(BigDecimal major, Currency currency) {
		Objects.requireNonNull(major, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		long units = major.movePointRight(currency.getDefaultFractionDigits()).longValueExact();
		return new Money(units, currency);
	}

	public long minorUnits() {
		return minorUnits;
	}

	public Currency currency() {
		return currency;
	}

	/** This amount expressed in major units, scaled to the currency's fraction digits. */
	public BigDecimal toBigDecimal() {
		return BigDecimal.valueOf(minorUnits, currency.getDefaultFractionDigits());
	}

	public Money plus(Money other) {
		requireSameCurrency(other);
		return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
	}

	public Money minus(Money other) {
		requireSameCurrency(other);
		return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
	}

	public Money negate() {
		return new Money(Math.negateExact(minorUnits), currency);
	}

	public boolean isZero() {
		return minorUnits == 0L;
	}

	public boolean isPositive() {
		return minorUnits > 0L;
	}

	public boolean isNegative() {
		return minorUnits < 0L;
	}

	public boolean isGreaterThanOrEqualTo(Money other) {
		return compareTo(other) >= 0;
	}

	@Override
	public int compareTo(Money other) {
		requireSameCurrency(other);
		return Long.compare(minorUnits, other.minorUnits);
	}

	private void requireSameCurrency(Money other) {
		Objects.requireNonNull(other, "other must not be null");
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException(
					"Currency mismatch: %s vs %s".formatted(currency, other.currency));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Money other)) {
			return false;
		}
		return minorUnits == other.minorUnits && currency.equals(other.currency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(minorUnits, currency);
	}

	@Override
	public String toString() {
		return "%s %s".formatted(toBigDecimal().toPlainString(), currency.getCurrencyCode());
	}
}
