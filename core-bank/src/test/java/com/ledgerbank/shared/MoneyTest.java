package com.ledgerbank.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency USD = Currency.getInstance("USD");
	private static final Currency EUR = Currency.getInstance("EUR");
	private static final Currency JPY = Currency.getInstance("JPY");

	@Nested
	class Construction {

		@Test
		void of_storesSignedMinorUnitsAndCurrency() {
			Money m = Money.of(150, USD);
			assertThat(m.minorUnits()).isEqualTo(150);
			assertThat(m.currency()).isEqualTo(USD);
		}

		@Test
		void zero_isZeroInGivenCurrency() {
			assertThat(Money.zero(USD).minorUnits()).isZero();
			assertThat(Money.zero(USD).isZero()).isTrue();
		}

		@Test
		void ofMajor_convertsDecimalToMinorUnits() {
			assertThat(Money.ofMajor(new BigDecimal("1.00"), USD)).isEqualTo(Money.of(100, USD));
			assertThat(Money.ofMajor(new BigDecimal("12.34"), USD)).isEqualTo(Money.of(1234, USD));
		}

		@Test
		void ofMajor_respectsCurrencyFractionDigits() {
			// JPY has 0 fraction digits: major 100 == 100 minor units.
			assertThat(Money.ofMajor(new BigDecimal("100"), JPY)).isEqualTo(Money.of(100, JPY));
		}

		@Test
		void ofMajor_rejectsMorePrecisionThanCurrencyAllows() {
			assertThatThrownBy(() -> Money.ofMajor(new BigDecimal("1.005"), USD))
					.isInstanceOf(ArithmeticException.class);
			assertThatThrownBy(() -> Money.ofMajor(new BigDecimal("100.5"), JPY))
					.isInstanceOf(ArithmeticException.class);
		}

		@Test
		void requiresCurrency() {
			assertThatThrownBy(() -> Money.of(1, null)).isInstanceOf(NullPointerException.class);
		}
	}

	@Nested
	class Arithmetic {

		@Test
		void plus_addsSameCurrency() {
			assertThat(Money.of(100, USD).plus(Money.of(50, USD))).isEqualTo(Money.of(150, USD));
		}

		@Test
		void minus_subtractsSameCurrency() {
			assertThat(Money.of(100, USD).minus(Money.of(30, USD))).isEqualTo(Money.of(70, USD));
		}

		@Test
		void minus_canGoNegative() {
			assertThat(Money.of(20, USD).minus(Money.of(50, USD)))
					.isEqualTo(Money.of(-30, USD));
		}

		@Test
		void negate_flipsSign() {
			assertThat(Money.of(40, USD).negate()).isEqualTo(Money.of(-40, USD));
			assertThat(Money.of(-40, USD).negate()).isEqualTo(Money.of(40, USD));
		}

		@Test
		void plus_rejectsMismatchedCurrency() {
			assertThatThrownBy(() -> Money.of(100, USD).plus(Money.of(100, EUR)))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void minus_rejectsMismatchedCurrency() {
			assertThatThrownBy(() -> Money.of(100, USD).minus(Money.of(100, EUR)))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void plus_throwsOnOverflow() {
			assertThatThrownBy(() -> Money.of(Long.MAX_VALUE, USD).plus(Money.of(1, USD)))
					.isInstanceOf(ArithmeticException.class);
		}
	}

	@Nested
	class Predicates {

		@Test
		void signPredicates() {
			assertThat(Money.of(1, USD).isPositive()).isTrue();
			assertThat(Money.of(1, USD).isNegative()).isFalse();
			assertThat(Money.of(-1, USD).isNegative()).isTrue();
			assertThat(Money.zero(USD).isPositive()).isFalse();
			assertThat(Money.zero(USD).isZero()).isTrue();
		}

		@Test
		void isGreaterThanOrEqualTo_comparesSameCurrency() {
			assertThat(Money.of(100, USD).isGreaterThanOrEqualTo(Money.of(100, USD))).isTrue();
			assertThat(Money.of(101, USD).isGreaterThanOrEqualTo(Money.of(100, USD))).isTrue();
			assertThat(Money.of(99, USD).isGreaterThanOrEqualTo(Money.of(100, USD))).isFalse();
		}

		@Test
		void comparison_rejectsMismatchedCurrency() {
			assertThatThrownBy(() -> Money.of(1, USD).isGreaterThanOrEqualTo(Money.of(1, EUR)))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	class Representation {

		@Test
		void toBigDecimal_returnsMajorUnitsScaledToCurrency() {
			assertThat(Money.of(100, USD).toBigDecimal()).isEqualByComparingTo("1.00");
			assertThat(Money.of(1234, USD).toBigDecimal()).isEqualByComparingTo("12.34");
			assertThat(Money.of(100, JPY).toBigDecimal()).isEqualByComparingTo("100");
		}

		@Test
		void equalsAndHashCode_dependOnAmountAndCurrency() {
			assertThat(Money.of(100, USD)).isEqualTo(Money.of(100, USD));
			assertThat(Money.of(100, USD)).hasSameHashCodeAs(Money.of(100, USD));
			assertThat(Money.of(100, USD)).isNotEqualTo(Money.of(100, EUR));
			assertThat(Money.of(100, USD)).isNotEqualTo(Money.of(101, USD));
		}

		@Test
		void toString_isHumanReadable() {
			assertThat(Money.of(1234, USD).toString()).contains("12.34").contains("USD");
		}
	}
}
