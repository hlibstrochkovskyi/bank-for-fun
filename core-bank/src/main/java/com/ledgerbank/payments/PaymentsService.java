package com.ledgerbank.payments;

import com.ledgerbank.fraud.FraudCheckRequest;
import com.ledgerbank.fraud.FraudClient;
import com.ledgerbank.fraud.RiskDecision;
import com.ledgerbank.ledger.LedgerLeg;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.ledger.PostingType;
import com.ledgerbank.ledger.RecordPostingCommand;
import com.ledgerbank.shared.Money;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Orchestrates money operations on top of the ledger. Each operation is idempotent
 * (a retry with the same key never double-charges) and ultimately a balanced
 * posting: a deposit moves money from the system clearing account into a customer
 * account; a transfer moves it between two accounts.
 */
@Service
public class PaymentsService {

	private final LedgerService ledger;
	private final SystemAccountService systemAccounts;
	private final IdempotencyService idempotency;
	private final FraudClient fraudClient;
	private final FraudSignals fraudSignals;
	private final HeldTransferService heldTransfers;

	public PaymentsService(LedgerService ledger, SystemAccountService systemAccounts,
			IdempotencyService idempotency, FraudClient fraudClient, FraudSignals fraudSignals,
			HeldTransferService heldTransfers) {
		this.ledger = ledger;
		this.systemAccounts = systemAccounts;
		this.idempotency = idempotency;
		this.fraudClient = fraudClient;
		this.fraudSignals = fraudSignals;
		this.heldTransfers = heldTransfers;
	}

	public PaymentResult deposit(DepositCommand command) {
		requirePositive(command.amount());
		String hash = requestHash("DEPOSIT", command.accountId().toString(), money(command.amount()));
		return idempotency.execute(command.idempotencyKey(), hash, () -> {
			var clearing = systemAccounts.clearingAccountFor(command.amount().currency());
			var posting = ledger.record(new RecordPostingCommand(
					PostingType.DEPOSIT, command.idempotencyKey(), command.description(),
					List.of(new LedgerLeg(clearing, command.amount().negate()),
							new LedgerLeg(command.accountId(), command.amount()))));
			return PaymentResult.completed(posting.id());
		});
	}

	public PaymentResult withdraw(WithdrawCommand command) {
		requirePositive(command.amount());
		String hash = requestHash("WITHDRAWAL", command.accountId().toString(), money(command.amount()));
		return idempotency.execute(command.idempotencyKey(), hash, () -> {
			var clearing = systemAccounts.clearingAccountFor(command.amount().currency());
			var posting = ledger.record(new RecordPostingCommand(
					PostingType.WITHDRAWAL, command.idempotencyKey(), command.description(),
					List.of(new LedgerLeg(command.accountId(), command.amount().negate()),
							new LedgerLeg(clearing, command.amount()))));
			return PaymentResult.completed(posting.id());
		});
	}

	public PaymentResult transfer(TransferCommand command) {
		requirePositive(command.amount());
		if (command.fromAccountId().equals(command.toAccountId())) {
			throw new IllegalArgumentException("cannot transfer to the same account");
		}
		String hash = requestHash("TRANSFER", command.fromAccountId().toString(),
				command.toAccountId().toString(), money(command.amount()));
		return idempotency.execute(command.idempotencyKey(), hash, () -> {
			// Fraud screening: a flagged transfer is held, not posted.
			RiskDecision decision = screen(command);
			if (decision.isHold()) {
				HeldTransfer held = heldTransfers.hold(command.fromAccountId(), command.toAccountId(),
						command.amount(), command.idempotencyKey(), command.description(), decision);
				return PaymentResult.held(held.id());
			}
			var posting = ledger.record(new RecordPostingCommand(
					PostingType.TRANSFER, command.idempotencyKey(), command.description(),
					List.of(new LedgerLeg(command.fromAccountId(), command.amount().negate()),
							new LedgerLeg(command.toAccountId(), command.amount()))));
			return PaymentResult.completed(posting.id());
		});
	}

	private RiskDecision screen(TransferCommand command) {
		FraudSignals.TransferSignals signals = fraudSignals.forTransfer(
				command.fromAccountId(), command.toAccountId());
		return fraudClient.score(new FraudCheckRequest(
				command.amount().minorUnits(), command.amount().currency().getCurrencyCode(),
				command.fromAccountId(), command.toAccountId(),
				signals.newPayee(), signals.recentTransferCount()));
	}

	public PaymentResult reverse(ReverseCommand command) {
		String hash = requestHash("REVERSAL", command.postingId().toString());
		return idempotency.execute(command.idempotencyKey(), hash, () ->
				PaymentResult.completed(ledger.reverse(command.postingId(), command.idempotencyKey(),
						command.reason()).id()));
	}

	private static void requirePositive(Money amount) {
		if (!amount.isPositive()) {
			throw new IllegalArgumentException("amount must be positive");
		}
	}

	private static String money(Money amount) {
		return amount.minorUnits() + amount.currency().getCurrencyCode();
	}

	/** A stable fingerprint of the request, used to detect idempotency-key reuse with a changed body. */
	private static String requestHash(String... parts) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(String.join("|", parts).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}
