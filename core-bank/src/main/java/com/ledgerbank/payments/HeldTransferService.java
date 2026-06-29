package com.ledgerbank.payments;

import com.ledgerbank.accounts.AccountRepository;
import com.ledgerbank.fraud.RiskDecision;
import com.ledgerbank.ledger.LedgerLeg;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.ledger.PostingType;
import com.ledgerbank.ledger.RecordPostingCommand;
import com.ledgerbank.shared.AccountNotFoundException;
import com.ledgerbank.shared.Money;
import com.ledgerbank.shared.events.TransferHeldEvent;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages transfers held by the fraud engine. A held transfer has not touched the
 * ledger; an admin either releases it (posts it) or rejects it (discards it).
 */
@Service
public class HeldTransferService {

	private final HeldTransferRepository heldTransfers;
	private final LedgerService ledger;
	private final AccountRepository accounts;
	private final ApplicationEventPublisher events;

	public HeldTransferService(HeldTransferRepository heldTransfers, LedgerService ledger,
			AccountRepository accounts, ApplicationEventPublisher events) {
		this.heldTransfers = heldTransfers;
		this.ledger = ledger;
		this.accounts = accounts;
		this.events = events;
	}

	/** Record a held transfer (called from the transfer flow when the fraud engine flags it). */
	@Transactional
	public HeldTransfer hold(UUID fromAccountId, UUID toAccountId, Money amount,
			String idempotencyKey, String description, RiskDecision decision) {
		UUID ownerId = accounts.findById(fromAccountId)
				.orElseThrow(() -> new AccountNotFoundException(fromAccountId)).ownerId();
		HeldTransfer held = heldTransfers.save(new HeldTransfer(ownerId, fromAccountId, toAccountId,
				amount.minorUnits(), amount.currency().getCurrencyCode(), idempotencyKey,
				decision.score(), decision.reasonSummary(), description));
		events.publishEvent(new TransferHeldEvent(held.id(), ownerId, fromAccountId, toAccountId,
				amount.minorUnits(), amount.currency().getCurrencyCode(), decision.score(),
				decision.reasonSummary()));
		return held;
	}

	@Transactional(readOnly = true)
	public List<HeldTransfer> listForOwner(UUID ownerId) {
		return heldTransfers.findByOwnerIdOrderByCreatedAtDesc(ownerId);
	}

	/** Every held transfer, newest first — the admin review queue. */
	@Transactional(readOnly = true)
	public List<HeldTransfer> listAll() {
		return heldTransfers.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
	}

	/** Release a held transfer: post it to the ledger. Returns the posting id. */
	@Transactional
	public UUID release(UUID heldTransferId) {
		HeldTransfer held = requirePending(heldTransferId);
		Money amount = Money.of(held.amount(), Currency.getInstance(held.currency()));
		var posting = ledger.record(new RecordPostingCommand(
				PostingType.TRANSFER, "held-release:" + held.id(), held.description(),
				List.of(new LedgerLeg(held.fromAccountId(), amount.negate()),
						new LedgerLeg(held.toAccountId(), amount))));
		held.markReleased();
		return posting.id();
	}

	/** Reject a held transfer: discard it without posting. */
	@Transactional
	public void reject(UUID heldTransferId) {
		requirePending(heldTransferId).markRejected();
	}

	private HeldTransfer requirePending(UUID heldTransferId) {
		HeldTransfer held = heldTransfers.findById(heldTransferId)
				.orElseThrow(() -> new HeldTransferNotFoundException(heldTransferId));
		if (held.status() != HeldTransferStatus.PENDING_REVIEW) {
			throw new IllegalStateException("held transfer %s is already %s"
					.formatted(heldTransferId, held.status()));
		}
		return held;
	}
}
