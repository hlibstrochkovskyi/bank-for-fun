package com.ledgerbank.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.ledger.InsufficientFundsException;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.shared.Money;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentsServiceIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	PaymentsService payments;

	@Autowired
	AccountService accounts;

	@Autowired
	LedgerService ledger;

	private Account customer() {
		return accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
	}

	private String key() {
		return UUID.randomUUID().toString();
	}

	@Test
	void deposit_increasesBalance() {
		Account a = customer();

		payments.deposit(new DepositCommand(a.id(), Money.of(1_000, USD), key(), "paycheck"));

		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(1_000, USD));
	}

	@Test
	void transfer_movesMoneyBetweenAccounts() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));

		payments.transfer(new TransferCommand(from.id(), to.id(), Money.of(250, USD), key(), "rent"));

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(750, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.of(250, USD));
	}

	@Test
	void transfer_beyondBalance_isRejected() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(100, USD), key(), null));

		assertThatThrownBy(() -> payments.transfer(
				new TransferCommand(from.id(), to.id(), Money.of(500, USD), key(), null)))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(100, USD));
	}

	@Test
	void deposit_isIdempotent_sameKeyDoesNotDoubleCharge() {
		Account a = customer();
		String key = key();
		DepositCommand cmd = new DepositCommand(a.id(), Money.of(1_000, USD), key, "once");

		PaymentResult first = payments.deposit(cmd);
		PaymentResult replay = payments.deposit(cmd);

		assertThat(replay.postingId()).isEqualTo(first.postingId());
		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(1_000, USD));
	}

	@Test
	void transfer_isIdempotent_sameKeyReplayReturnsSameResult() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));
		String key = key();
		TransferCommand cmd = new TransferCommand(from.id(), to.id(), Money.of(400, USD), key, null);

		PaymentResult first = payments.transfer(cmd);
		PaymentResult replay = payments.transfer(cmd);

		assertThat(replay.postingId()).isEqualTo(first.postingId());
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(600, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.of(400, USD));
	}

	@Test
	void sameKeyWithDifferentRequest_isConflict() {
		Account a = customer();
		String key = key();
		payments.deposit(new DepositCommand(a.id(), Money.of(100, USD), key, null));

		assertThatThrownBy(() -> payments.deposit(new DepositCommand(a.id(), Money.of(200, USD), key, null)))
				.isInstanceOf(IdempotencyConflictException.class);

		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(100, USD));
	}

	@Test
	void transfer_toSameAccount_isRejected() {
		Account a = customer();
		payments.deposit(new DepositCommand(a.id(), Money.of(100, USD), key(), null));

		assertThatThrownBy(() -> payments.transfer(
				new TransferCommand(a.id(), a.id(), Money.of(10, USD), key(), null)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void withdraw_decreasesBalance() {
		Account a = customer();
		payments.deposit(new DepositCommand(a.id(), Money.of(1_000, USD), key(), null));

		payments.withdraw(new WithdrawCommand(a.id(), Money.of(300, USD), key(), "atm"));

		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(700, USD));
	}

	@Test
	void withdraw_beyondBalance_isRejected() {
		Account a = customer();
		payments.deposit(new DepositCommand(a.id(), Money.of(100, USD), key(), null));

		assertThatThrownBy(() -> payments.withdraw(
				new WithdrawCommand(a.id(), Money.of(500, USD), key(), null)))
				.isInstanceOf(InsufficientFundsException.class);

		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(100, USD));
	}

	@Test
	void withdraw_isIdempotent() {
		Account a = customer();
		payments.deposit(new DepositCommand(a.id(), Money.of(1_000, USD), key(), null));
		String key = key();
		WithdrawCommand cmd = new WithdrawCommand(a.id(), Money.of(200, USD), key, null);

		PaymentResult first = payments.withdraw(cmd);
		PaymentResult replay = payments.withdraw(cmd);

		assertThat(replay.postingId()).isEqualTo(first.postingId());
		assertThat(ledger.balanceOf(a.id())).isEqualTo(Money.of(800, USD));
	}

	@Test
	void deposit_nonPositiveAmount_isRejected() {
		Account a = customer();

		assertThatThrownBy(() -> payments.deposit(
				new DepositCommand(a.id(), Money.of(0, USD), key(), null)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> payments.deposit(
				new DepositCommand(a.id(), Money.of(-5, USD), key(), null)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
