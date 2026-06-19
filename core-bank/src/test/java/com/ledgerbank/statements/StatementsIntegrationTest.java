package com.ledgerbank.statements;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.TransferCommand;
import com.ledgerbank.shared.Money;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StatementsIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	StatementsService statements;

	@Autowired
	AccountService accounts;

	@Autowired
	PaymentsService payments;

	private String key() {
		return UUID.randomUUID().toString();
	}

	@Test
	void statement_summarisesOpeningClosingAndTotalsForThePeriod() {
		Account a = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
		Account other = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
		payments.deposit(new DepositCommand(a.id(), Money.of(1_000, USD), key(), "salary"));
		payments.transfer(new TransferCommand(a.id(), other.id(), Money.of(200, USD), key(), "rent"));

		OffsetDateTime from = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
		OffsetDateTime to = from.plusDays(1);

		Statement statement = statements.statement(a.id(), from, to);

		assertThat(statement.openingBalance()).isEqualTo(Money.zero(USD));
		assertThat(statement.totalCredits()).isEqualTo(Money.of(1_000, USD));
		assertThat(statement.totalDebits()).isEqualTo(Money.of(200, USD));
		assertThat(statement.closingBalance()).isEqualTo(Money.of(800, USD));
		assertThat(statement.transactions()).hasSize(2);
	}

	@Test
	void statement_excludesEntriesOutsideThePeriod() {
		Account a = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
		payments.deposit(new DepositCommand(a.id(), Money.of(500, USD), key(), null));

		// A period entirely in the past contains nothing; opening == closing == 0.
		OffsetDateTime from = LocalDate.now(ZoneOffset.UTC).minusDays(10)
				.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
		OffsetDateTime to = from.plusDays(1);

		Statement statement = statements.statement(a.id(), from, to);

		assertThat(statement.transactions()).isEmpty();
		assertThat(statement.openingBalance()).isEqualTo(Money.zero(USD));
		assertThat(statement.closingBalance()).isEqualTo(Money.zero(USD));
	}
}
