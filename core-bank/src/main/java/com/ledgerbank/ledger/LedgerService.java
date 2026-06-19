package com.ledgerbank.ledger;

import com.ledgerbank.shared.AccountNotFoundException;
import com.ledgerbank.shared.Money;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

	public LedgerService(AccountBalanceRepository balances, PostingRepository postings,
			LedgerEntryRepository entries) {
		this.balances = balances;
		this.postings = postings;
		this.entries = entries;
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
		List<LedgerLeg> legs = command.legs();
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
		for (Map.Entry<UUID, Long> delta : deltas.entrySet()) {
			AccountBalance balance = locked.get(delta.getKey());
			long projected = Math.addExact(balance.balance(), delta.getValue());
			if (balance.minBalance() != null && projected < balance.minBalance()) {
				throw new InsufficientFundsException(delta.getKey(), projected, balance.minBalance());
			}
		}

		// Persist the immutable posting + entries, then move the snapshots.
		Posting posting = postings.save(new Posting(command.type(), command.idempotencyKey(),
				command.description()));
		for (LedgerLeg leg : legs) {
			entries.save(new LedgerEntry(posting.id(), leg.accountId(), leg.amount().minorUnits(),
					leg.amount().currency().getCurrencyCode()));
		}
		deltas.forEach((accountId, delta) -> locked.get(accountId).apply(delta));
		return posting;
	}

	/** The current snapshot balance of an account. */
	@Transactional(readOnly = true)
	public Money balanceOf(UUID accountId) {
		AccountBalance balance = balances.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
		return Money.of(balance.balance(), Currency.getInstance(balance.currency()));
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
