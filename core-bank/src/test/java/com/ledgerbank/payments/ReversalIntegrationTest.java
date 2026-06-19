package com.ledgerbank.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.ledger.ReversalNotAllowedException;
import com.ledgerbank.shared.Money;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ReversalIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	PaymentsService payments;

	@Autowired
	AccountService accounts;

	@Autowired
	LedgerService ledger;

	@Autowired
	JdbcTemplate jdbc;

	private Account customer() {
		return accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
	}

	private String key() {
		return UUID.randomUUID().toString();
	}

	@Test
	void reverse_restoresBalances_andLeavesOriginalIntact() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));
		UUID transferPosting = payments.transfer(
				new TransferCommand(from.id(), to.id(), Money.of(300, USD), key(), null)).postingId();

		long entriesBefore = jdbc.queryForObject(
				"SELECT count(*) FROM ledger_entry WHERE posting_id = ?", Long.class, transferPosting);

		payments.reverse(new ReverseCommand(transferPosting, key(), "customer dispute"));

		// Balances are restored by the compensating posting.
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(1_000, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.zero(USD));

		// The original posting and its entries are untouched (immutability).
		long entriesAfter = jdbc.queryForObject(
				"SELECT count(*) FROM ledger_entry WHERE posting_id = ?", Long.class, transferPosting);
		assertThat(entriesAfter).isEqualTo(entriesBefore);

		// A REVERSAL posting links back to the original.
		Long reversalCount = jdbc.queryForObject(
				"SELECT count(*) FROM posting WHERE reverses_posting_id = ? AND type = 'REVERSAL'",
				Long.class, transferPosting);
		assertThat(reversalCount).isEqualTo(1);

		// Snapshot still equals the ledger re-derived from entries.
		for (Account a : java.util.List.of(from, to)) {
			assertThat(ledger.balanceOf(a.id())).isEqualTo(ledger.deriveBalanceFromEntries(a.id()));
		}
	}

	@Test
	void reverse_twice_isRejected() {
		Account a = customer();
		UUID deposit = payments.deposit(new DepositCommand(a.id(), Money.of(500, USD), key(), null)).postingId();
		payments.reverse(new ReverseCommand(deposit, key(), null));

		assertThatThrownBy(() -> payments.reverse(new ReverseCommand(deposit, key(), null)))
				.isInstanceOf(ReversalNotAllowedException.class);
	}

	@Test
	void reverse_isIdempotent_sameKeyReturnsSameResult() {
		Account a = customer();
		UUID deposit = payments.deposit(new DepositCommand(a.id(), Money.of(500, USD), key(), null)).postingId();
		String key = key();
		ReverseCommand cmd = new ReverseCommand(deposit, key, null);

		PaymentResult first = payments.reverse(cmd);
		PaymentResult replay = payments.reverse(cmd);

		assertThat(replay.postingId()).isEqualTo(first.postingId());
		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.zero(USD));
	}

	@Test
	void reverse_mayPushCustomerAccountNegative_bypassingOverdraftFloor() {
		Account a = customer();
		Account b = customer();
		UUID deposit = payments.deposit(new DepositCommand(a.id(), Money.of(100, USD), key(), null)).postingId();
		// Move the funds away, then reverse the original deposit: the clawback overdraws a.
		payments.transfer(new TransferCommand(a.id(), b.id(), Money.of(100, USD), key(), null));

		payments.reverse(new ReverseCommand(deposit, key(), "chargeback"));

		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(-100, USD));
	}
}
