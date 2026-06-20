package com.ledgerbank.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.fraud.FraudAction;
import com.ledgerbank.fraud.FraudClient;
import com.ledgerbank.fraud.RiskDecision;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.TransferCommand;
import com.ledgerbank.shared.Money;
import java.time.Duration;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotificationIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@RegisterExtension
	static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

	@DynamicPropertySource
	static void mailProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", () -> "127.0.0.1");
		registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
	}

	@MockitoBean
	FraudClient fraudClient;

	@Autowired
	PaymentsService payments;

	@Autowired
	AccountService accounts;

	private Account customer(UUID owner) {
		return accounts.openCustomerAccount(owner, AccountType.CHECKING, USD);
	}

	@Test
	void heldTransfer_emailsTheCustomerViaTheNotificationWorker() throws Exception {
		when(fraudClient.score(any()))
				.thenReturn(new RiskDecision(0.9, FraudAction.HOLD, List.of("high amount")));
		UUID owner = UUID.randomUUID();
		Account from = customer(owner);
		Account to = customer(UUID.randomUUID());
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), UUID.randomUUID().toString(), null));

		payments.transfer(new TransferCommand(from.id(), to.id(), Money.of(300, USD),
				UUID.randomUUID().toString(), "rent"));

		// Event -> RabbitMQ -> notification worker -> email (asynchronously).
		await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
				assertThat(greenMail.getReceivedMessages()).hasSize(1));

		var email = greenMail.getReceivedMessages()[0];
		assertThat(email.getSubject()).contains("being reviewed");
		assertThat(email.getAllRecipients()[0].toString()).isEqualTo(owner + "@ledger-bank.local");
	}
}
