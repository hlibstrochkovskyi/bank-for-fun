package com.ledgerbank.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.shared.Money;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LedgerServiceIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	LedgerService ledger;

	@Autowired
	AccountService accounts;

	private Account customer() {
		return accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
	}

	private Account clearing() {
		return accounts.openSystemAccount(AccountType.SYSTEM_CLEARING, USD);
	}

	/** A balanced posting that funds {@code account} from the (unbounded) clearing account. */
	private void fund(Account account, long minorUnits) {
		ledger.record(new RecordPostingCommand(PostingType.DEPOSIT, "fund-" + UUID.randomUUID(), "seed",
				List.of(new LedgerLeg(clearing().id(), Money.of(-minorUnits, USD)),
						new LedgerLeg(account.id(), Money.of(minorUnits, USD)))));
	}

	@Test
	void openCustomerAccount_startsAtZero() {
		Account a = customer();
		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.zero(USD));
	}

	@Test
	void recordsBalancedPosting_updatesSnapshotBalances() {
		Account from = customer();
		Account to = customer();
		fund(from, 1_000);

		ledger.record(new RecordPostingCommand(PostingType.TRANSFER, UUID.randomUUID().toString(), "rent",
				List.of(new LedgerLeg(from.id(), Money.of(-400, USD)),
						new LedgerLeg(to.id(), Money.of(400, USD)))));

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(600, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.of(400, USD));
	}

	@Test
	void snapshotBalance_matchesBalanceDerivedFromEntries() {
		Account from = customer();
		Account to = customer();
		fund(from, 5_000);
		ledger.record(new RecordPostingCommand(PostingType.TRANSFER, UUID.randomUUID().toString(), null,
				List.of(new LedgerLeg(from.id(), Money.of(-1_250, USD)),
						new LedgerLeg(to.id(), Money.of(1_250, USD)))));

		for (Account a : List.of(from, to)) {
			assertThat(ledger.balanceOf(a.id())).isEqualTo(ledger.deriveBalanceFromEntries(a.id()));
		}
	}

	@Test
	void rejectsUnbalancedLegs_andPersistsNothing() {
		Account from = customer();
		Account to = customer();
		fund(from, 1_000);

		assertThatThrownBy(() -> ledger.record(
				new RecordPostingCommand(PostingType.TRANSFER, UUID.randomUUID().toString(), "bad",
						List.of(new LedgerLeg(from.id(), Money.of(-400, USD)),
								new LedgerLeg(to.id(), Money.of(300, USD))))))
				.isInstanceOf(UnbalancedPostingException.class);

		// Balances unchanged by the rejected posting.
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(1_000, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.zero(USD));
	}

	@Test
	void rejectsOverdraftOnCustomerAccount() {
		Account from = customer();
		Account to = customer();
		fund(from, 100);

		assertThatThrownBy(() -> ledger.record(
				new RecordPostingCommand(PostingType.TRANSFER, UUID.randomUUID().toString(), "overdraft",
						List.of(new LedgerLeg(from.id(), Money.of(-500, USD)),
								new LedgerLeg(to.id(), Money.of(500, USD))))))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(100, USD));
	}

	@Test
	void systemAccountMayGoNegative() {
		Account a = customer();
		fund(a, 2_000); // clearing account goes to -2000, which is allowed

		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(2_000, USD));
	}
}
