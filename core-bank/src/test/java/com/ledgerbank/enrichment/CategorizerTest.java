package com.ledgerbank.enrichment;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerbank.enrichment.Categorizer.Enrichment;
import org.junit.jupiter.api.Test;

class CategorizerTest {

	private final Categorizer categorizer = new Categorizer();

	@Test
	void salary_isIncome_andMerchantIsAfterDash() {
		Enrichment e = categorizer.categorize("DEPOSIT", "Salary — Atlas Studio");
		assertThat(e.category()).isEqualTo(TransactionCategory.INCOME);
		assertThat(e.merchant()).isEqualTo("Atlas Studio");
	}

	@Test
	void rent_isHousing() {
		assertThat(categorizer.categorize("WITHDRAWAL", "Rent — Oak Property").category())
				.isEqualTo(TransactionCategory.HOUSING);
	}

	@Test
	void wholeFoods_isGroceries() {
		assertThat(categorizer.categorize("WITHDRAWAL", "Whole Foods Market").category())
				.isEqualTo(TransactionCategory.GROCERIES);
	}

	@Test
	void coffee_isDining() {
		assertThat(categorizer.categorize("WITHDRAWAL", "Blue Bottle Coffee").category())
				.isEqualTo(TransactionCategory.DINING);
	}

	@Test
	void spotify_isSubscriptions() {
		assertThat(categorizer.categorize("WITHDRAWAL", "Spotify").category())
				.isEqualTo(TransactionCategory.SUBSCRIPTIONS);
	}

	@Test
	void conEdison_isUtilities() {
		assertThat(categorizer.categorize("WITHDRAWAL", "Con Edison").category())
				.isEqualTo(TransactionCategory.UTILITIES);
	}

	@Test
	void metro_isTransport() {
		assertThat(categorizer.categorize("WITHDRAWAL", "Metro Transit").category())
				.isEqualTo(TransactionCategory.TRANSPORT);
	}

	@Test
	void transferType_isTransfer() {
		assertThat(categorizer.categorize("TRANSFER", "to savings").category())
				.isEqualTo(TransactionCategory.TRANSFER);
	}

	@Test
	void refund_isShopping_evenAsDeposit() {
		assertThat(categorizer.categorize("DEPOSIT", "Refund — Uniqlo").category())
				.isEqualTo(TransactionCategory.SHOPPING);
	}

	@Test
	void blankDescription_hasNullMerchant() {
		Enrichment e = categorizer.categorize("DEPOSIT", "");
		assertThat(e.merchant()).isNull();
		assertThat(e.category()).isEqualTo(TransactionCategory.INCOME);
	}
}
