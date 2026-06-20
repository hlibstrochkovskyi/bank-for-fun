package com.ledgerbank.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import com.ledgerbank.shared.events.TransferHeldEvent;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(MessagingIntegrationTest.TestQueueConfig.class)
class MessagingIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");
	static final String TEST_QUEUE = "test.transfer.held";

	@TestConfiguration
	static class TestQueueConfig {
		@Bean
		Queue heldTestQueue() {
			return new Queue(TEST_QUEUE, false);
		}

		@Bean
		Binding heldTestBinding(Queue heldTestQueue, TopicExchange eventsExchange) {
			return BindingBuilder.bind(heldTestQueue).to(eventsExchange).with(EventRouting.TRANSFER_HELD);
		}
	}

	@MockitoBean
	FraudClient fraudClient;

	@Autowired
	PaymentsService payments;

	@Autowired
	AccountService accounts;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	RabbitAdmin rabbitAdmin;

	private Account customer() {
		return accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
	}

	@Test
	void heldTransfer_publishesEventToRabbitMq() {
		rabbitAdmin.purgeQueue(TEST_QUEUE, false);
		when(fraudClient.score(any()))
				.thenReturn(new RiskDecision(0.9, FraudAction.HOLD, List.of("high amount")));
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), UUID.randomUUID().toString(), null));

		payments.transfer(new TransferCommand(from.id(), to.id(), Money.of(300, USD),
				UUID.randomUUID().toString(), "rent"));

		Object message = rabbitTemplate.receiveAndConvert(TEST_QUEUE, 5_000);
		assertThat(message).isInstanceOf(TransferHeldEvent.class);
		TransferHeldEvent event = (TransferHeldEvent) message;
		assertThat(event.fromAccountId()).isEqualTo(from.id());
		assertThat(event.toAccountId()).isEqualTo(to.id());
		assertThat(event.amount()).isEqualTo(300L);
		assertThat(event.reason()).contains("high amount");
	}
}
