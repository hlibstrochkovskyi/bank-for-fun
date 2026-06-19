package com.ledgerbank.standingorders;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.shared.Money;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StandingOrderIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	StandingOrderService standingOrders;

	@Autowired
	StandingOrderRepository repository;

	@Autowired
	AccountService accounts;

	@Autowired
	PaymentsService payments;

	@Autowired
	LedgerService ledger;

	private Account customer() {
		return accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
	}

	@Test
	void runDue_executesDueOrderOnce_andAdvancesSchedule() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD),
				UUID.randomUUID().toString(), null));
		OffsetDateTime firstRun = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS).minusMinutes(1);
		StandingOrder order = standingOrders.create(new CreateStandingOrderCommand(
				from.ownerId(), from.id(), to.id(), Money.of(100, USD), 7, firstRun, "weekly"));

		int attempted = standingOrders.runDue();
		assertThat(attempted).isEqualTo(1);
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(900, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.of(100, USD));

		StandingOrder after = repository.findById(order.id()).orElseThrow();
		assertThat(after.nextRunAt().isEqual(firstRun.plusDays(7))).isTrue();

		// Not due anymore: a second sweep does nothing.
		assertThat(standingOrders.runDue()).isZero();
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(900, USD));
	}

	@Test
	void runDue_skipsWhenInsufficientFunds_butStillAdvances() {
		Account from = customer(); // no funds
		Account to = customer();
		OffsetDateTime firstRun = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS).minusMinutes(1);
		StandingOrder order = standingOrders.create(new CreateStandingOrderCommand(
				from.ownerId(), from.id(), to.id(), Money.of(100, USD), 30, firstRun, null));

		standingOrders.runDue();

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.zero(USD));
		assertThat(repository.findById(order.id()).orElseThrow().nextRunAt()
				.isEqual(firstRun.plusDays(30))).isTrue();
	}
}
