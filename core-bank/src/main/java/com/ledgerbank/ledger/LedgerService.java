package com.ledgerbank.ledger;

import com.ledgerbank.shared.AccountNotFoundException;
import com.ledgerbank.shared.Money;
import com.ledgerbank.shared.events.MoneyPostedEvent;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The core of the bank: the only component that mutates money. It records balanced,
 * immutable postings and keeps each account's derived balance snapshot in step
 * within the same transaction.
 *
 * <p>Concurrency safety (ADR-0004): the affected balance rows are locked with
 * {@code SELECT ... FOR UPDATE} in a deterministic order (sorted by account id),
 * which makes deadlocks impossible. The overdraft floor is checked under that lock.
 */
@Service
public class LedgerService {

	private final AccountBalanceRepository balances;
	private final PostingRepository postings;
	private final LedgerEntryRepository entries;
	private final org.springframework.context.ApplicationEventPublisher events;

	public LedgerService(AccountBalanceRepository balances, PostingRepository postings,
			LedgerEntryRepository entries, org.springframework.context.ApplicationEventPublisher events) {
		this.balances = balances;
		this.postings = postings;
		this.entries = entries;
		this.events = events;
	}

	/** Initialise an account's balance snapshot at zero. {@code minBalance == null} = unbounded. */
	@Transactional
	public void openBalance(UUID accountId, Currency currency, Long minBalance) {
		balances.save(AccountBalance.open(accountId, currency.getCurrencyCode(), minBalance));
	}

	/**
	 * Record a balanced posting and atomically update the affected balance snapshots.
	 *
	 * @throws UnbalancedPostingException  if the legs do not sum to zero per currency
	 * @throws InsufficientFundsException  if a leg would breach an account's minimum
	 * @throws CurrencyMismatchException   if a leg's currency differs from its account
	 * @throws AccountNotFoundException    if a leg references an unknown account
	 */
	@Transactional
	public Posting record(RecordPostingCommand command) {
		return write(command.type(), command.idempotencyKey(), command.description(),
				command.legs(), null, true);
	}

	/**
	 * Reverse a posting by recording its exact inverse as a new {@code REVERSAL}
	 * posting (the original is never edited or deleted). A posting may be reversed at
	 * most once, and a reversal cannot itself be reversed. Reversals are corrections,
	 * so they bypass overdraft floors (the inverse may push an account negative).
	 *
	 * @throws PostingNotFoundException    if the original posting does not exist
	 * @throws ReversalNotAllowedException if it is already reversed or is a reversal
	 */
	@Transactional
	public Posting reverse(UUID originalPostingId, String idempotencyKey, String reason) {
		Posting original = postings.findById(originalPostingId)
				.orElseThrow(() -> new PostingNotFoundException(originalPostingId));
		if (original.type() == PostingType.REVERSAL) {
			throw new ReversalNotAllowedException(originalPostingId, "a reversal cannot be reversed");
		}
		if (postings.existsByReversesPostingId(originalPostingId)) {
			throw new ReversalNotAllowedException(originalPostingId, "already reversed");
		}
		List<LedgerLeg> inverseLegs = entries.findByPostingId(originalPostingId).stream()
				.map(entry -> new LedgerLeg(entry.accountId(),
						Money.of(Math.negateExact(entry.amount()), Currency.getInstance(entry.currency()))))
				.toList();
		return write(PostingType.REVERSAL, idempotencyKey,
				reason != null ? reason : "reversal of " + originalPostingId,
				inverseLegs, originalPostingId, false);
	}

	private Posting write(PostingType type, String idempotencyKey, String description,
			List<LedgerLeg> legs, UUID reversesPostingId, boolean enforceFloors) {
		validateBalanced(legs);

		// Lock the affected balances in a deterministic order to prevent deadlocks.
		List<UUID> lockOrder = legs.stream().map(LedgerLeg::accountId).distinct().sorted().toList();
		Map<UUID, AccountBalance> locked = new LinkedHashMap<>();
		for (UUID accountId : lockOrder) {
			locked.put(accountId, balances.findByIdForUpdate(accountId)
					.orElseThrow(() -> new AccountNotFoundException(accountId)));
		}

		// Net change per account; verify each leg's currency matches its account.
		Map<UUID, Long> deltas = new HashMap<>();
		for (LedgerLeg leg : legs) {
			AccountBalance balance = locked.get(leg.accountId());
			String legCurrency = leg.amount().currency().getCurrencyCode();
			if (!balance.currency().equals(legCurrency)) {
				throw new CurrencyMismatchException(leg.accountId(), balance.currency(), legCurrency);
			}
			deltas.merge(leg.accountId(), leg.amount().minorUnits(), Long::sum);
		}

		// Enforce overdraft floors under the lock, before writing anything.
		if (enforceFloors) {
			for (Map.Entry<UUID, Long> delta : deltas.entrySet()) {
				AccountBalance balance = locked.get(delta.getKey());
				long projected = Math.addExact(balance.balance(), delta.getValue());
				if (balance.minBalance() != null && projected < balance.minBalance()) {
					throw new InsufficientFundsException(delta.getKey(), projected, balance.minBalance());
				}
			}
		}

		// Persist the immutable posting + entries, then move the snapshots.
		Posting posting = postings.save(new Posting(type, idempotencyKey, description, reversesPostingId));
		for (LedgerLeg leg : legs) {
			entries.save(new LedgerEntry(posting.id(), leg.accountId(), leg.amount().minorUnits(),
					leg.amount().currency().getCurrencyCode()));
		}
		deltas.forEach((accountId, delta) -> locked.get(accountId).apply(delta));
		events.publishEvent(new MoneyPostedEvent(posting.id(), type.name(), description));
		return posting;
	}

	/** The current snapshot balance of an account. */
	@Transactional(readOnly = true)
	public Money balanceOf(UUID accountId) {
		AccountBalance balance = balances.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
		return Money.of(balance.balance(), Currency.getInstance(balance.currency()));
	}

	/** Most-recent-first transaction history for an account (entries + posting metadata). */
	@Transactional(readOnly = true)
	public List<LedgerTransaction> history(UUID accountId, int limit) {
		if (!balances.existsById(accountId)) {
			throw new AccountNotFoundException(accountId);
		}
		List<LedgerEntry> entryRows = entries.findByAccountIdOrderByIdDesc(accountId, Limit.of(limit));
		Map<UUID, Posting> postingsById = postings
				.findAllById(entryRows.stream().map(LedgerEntry::postingId).distinct().toList())
				.stream().collect(Collectors.toMap(Posting::id, Function.identity()));
		return entryRows.stream().map(entry -> {
			Posting posting = postingsById.get(entry.postingId());
			return new LedgerTransaction(entry.id(), entry.postingId(), posting.type(),
					Money.of(entry.amount(), Currency.getInstance(entry.currency())),
					posting.description(), entry.createdAt());
		}).toList();
	}

	/** The balance re-derived from the immutable entries — used to reconcile the snapshot. */
	@Transactional(readOnly = true)
	public Money deriveBalanceFromEntries(UUID accountId) {
		AccountBalance balance = balances.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
		long sum = entries.sumAmountByAccountId(accountId);
		return Money.of(sum, Currency.getInstance(balance.currency()));
	}

	private void validateBalanced(List<LedgerLeg> legs) {
		if (legs.size() < 2) {
			throw new UnbalancedPostingException("a posting needs at least two entries");
		}
		Map<String, Long> perCurrency = new HashMap<>();
		for (LedgerLeg leg : legs) {
			if (leg.amount().isZero()) {
				throw new UnbalancedPostingException("zero-amount entries are not allowed");
			}
			perCurrency.merge(leg.amount().currency().getCurrencyCode(), leg.amount().minorUnits(),
					Long::sum);
		}
		if (perCurrency.values().stream().anyMatch(sum -> sum != 0L)) {
			throw new UnbalancedPostingException("entries do not sum to zero per currency");
		}
	}
}
