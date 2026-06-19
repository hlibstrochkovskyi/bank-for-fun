package com.ledgerbank.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.shared.Money;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	AccountService accounts;

	@Autowired
	PaymentsService payments;

	@Autowired
	AuditLogRepository auditLog;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void stateChangesAreAudited() {
		Account a = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
		var deposit = payments.deposit(new DepositCommand(a.id(), Money.of(1_000, USD),
				UUID.randomUUID().toString(), null));

		assertThat(auditLog.findByTargetTypeAndTargetIdOrderByIdAsc("ACCOUNT", a.id().toString()))
				.singleElement()
				.satisfies(entry -> assertThat(entry.action()).isEqualTo("ACCOUNT_OPENED"));

		assertThat(auditLog.findByTargetTypeAndTargetIdOrderByIdAsc("POSTING", deposit.postingId().toString()))
				.singleElement()
				.satisfies(entry -> assertThat(entry.action()).isEqualTo("MONEY_POSTED"));
	}

	@Test
	void auditLogIsAppendOnly_updateAndDeleteAreRejected() {
		Account a = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
		Long id = auditLog.findByTargetTypeAndTargetIdOrderByIdAsc("ACCOUNT", a.id().toString())
				.getFirst().id();

		assertThatThrownBy(() -> jdbc.update("UPDATE audit_log SET action = 'TAMPERED' WHERE id = ?", id))
				.hasMessageContaining("append-only");
		assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE id = ?", id))
				.hasMessageContaining("append-only");
	}
}
