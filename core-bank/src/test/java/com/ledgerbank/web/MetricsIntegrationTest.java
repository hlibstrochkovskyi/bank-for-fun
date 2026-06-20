package com.ledgerbank.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.shared.Money;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MetricsIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	AccountService accounts;

	@Autowired
	PaymentsService payments;

	@Autowired
	MeterRegistry registry;

	@Test
	void recordingAPosting_incrementsTheLedgerMetricTaggedByType() {
		double before = depositCount();
		var account = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
		payments.deposit(new DepositCommand(account.id(), Money.of(100, USD),
				UUID.randomUUID().toString(), null));

		assertThat(depositCount()).isEqualTo(before + 1.0);
	}

	private double depositCount() {
		var counter = registry.find("ledger.postings").tag("type", "DEPOSIT").counter();
		return counter == null ? 0.0 : counter.count();
	}
}
