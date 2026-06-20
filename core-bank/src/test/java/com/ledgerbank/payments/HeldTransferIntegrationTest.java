package com.ledgerbank.payments;

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
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.shared.Money;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class HeldTransferIntegrationTest extends AbstractIntegrationTest {

	private static final Currency USD = Currency.getInstance("USD");

	@MockitoBean
	FraudClient fraudClient;

	@Autowired
	PaymentsService payments;

	@Autowired
	HeldTransferService heldTransfers;

	@Autowired
	HeldTransferRepository heldTransferRepository;

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

	private void fraudReturns(FraudAction action) {
		when(fraudClient.score(any())).thenReturn(
				new RiskDecision(action == FraudAction.HOLD ? 0.9 : 0.0, action, List.of("test rule")));
	}

	@Test
	void flaggedTransfer_isHeld_notPosted() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));
		fraudReturns(FraudAction.HOLD);

		PaymentResult result = payments.transfer(
				new TransferCommand(from.id(), to.id(), Money.of(300, USD), key(), "rent"));

		assertThat(result.status()).isEqualTo(PaymentStatus.HELD);
		assertThat(result.heldTransferId()).isNotNull();
		assertThat(result.postingId()).isNull();
		// Money did not move.
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(1_000, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.zero(USD));
		// A pending held transfer exists for the owner.
		assertThat(heldTransferRepository.findById(result.heldTransferId()).orElseThrow().status())
				.isEqualTo(HeldTransferStatus.PENDING_REVIEW);
	}

	@Test
	void releasingAHeldTransfer_postsIt() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));
		fraudReturns(FraudAction.HOLD);
		UUID heldId = payments.transfer(
				new TransferCommand(from.id(), to.id(), Money.of(300, USD), key(), null)).heldTransferId();

		heldTransfers.release(heldId);

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(700, USD));
		assertThat(ledger.balanceOf(to.id())).isEqualTo(Money.of(300, USD));
		assertThat(heldTransferRepository.findById(heldId).orElseThrow().status())
				.isEqualTo(HeldTransferStatus.RELEASED);
	}

	@Test
	void rejectingAHeldTransfer_discardsIt() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));
		fraudReturns(FraudAction.HOLD);
		UUID heldId = payments.transfer(
				new TransferCommand(from.id(), to.id(), Money.of(300, USD), key(), null)).heldTransferId();

		heldTransfers.reject(heldId);

		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(1_000, USD));
		assertThat(heldTransferRepository.findById(heldId).orElseThrow().status())
				.isEqualTo(HeldTransferStatus.REJECTED);
	}

	@Test
	void allowedTransfer_postsNormally() {
		Account from = customer();
		Account to = customer();
		payments.deposit(new DepositCommand(from.id(), Money.of(1_000, USD), key(), null));
		fraudReturns(FraudAction.ALLOW);

		PaymentResult result = payments.transfer(
				new TransferCommand(from.id(), to.id(), Money.of(300, USD), key(), null));

		assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
		assertThat(result.postingId()).isNotNull();
		assertThat(ledger.balanceOf(from.id())).isEqualTo(Money.of(700, USD));
	}
}
