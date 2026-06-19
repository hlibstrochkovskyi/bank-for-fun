package com.ledgerbank.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.TransferCommand;
import com.ledgerbank.shared.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The flagship test: thousands of concurrent transfers between accounts, after
 * which money must be conserved to the cent. This is the evidence that the ledger's
 * locking and balance handling are correct under real contention, not just in
 * theory. See ADR-0004 (pessimistic locking + deterministic lock ordering).
 */
class MoneyConservationConcurrencyTest extends AbstractIntegrationTest {

	private static final java.util.Currency USD = java.util.Currency.getInstance("USD");

	private static final int ACCOUNTS = 20;
	private static final long SEED_PER_ACCOUNT = 10_000; // minor units
	private static final int THREADS = 8;
	private static final int TRANSFERS_PER_THREAD = 250;

	@Autowired
	PaymentsService payments;

	@Autowired
	AccountService accounts;

	@Autowired
	LedgerService ledger;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void concurrentTransfers_conserveMoney() throws Exception {
		// Seed N accounts with a known balance and record the total.
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < ACCOUNTS; i++) {
			Account a = accounts.openCustomerAccount(UUID.randomUUID(), AccountType.CHECKING, USD);
			payments.deposit(new DepositCommand(a.id(), Money.of(SEED_PER_ACCOUNT, USD),
					UUID.randomUUID().toString(), "seed"));
			ids.add(a.id());
		}
		long initialTotal = (long) ACCOUNTS * SEED_PER_ACCOUNT;

		AtomicInteger committed = new AtomicInteger();
		AtomicInteger rejected = new AtomicInteger();

		// Fire M threads, each doing K random transfers between random accounts.
		try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
			List<Future<?>> futures = new ArrayList<>();
			for (int t = 0; t < THREADS; t++) {
				futures.add(pool.submit((Callable<Void>) () -> {
					ThreadLocalRandom rnd = ThreadLocalRandom.current();
					for (int i = 0; i < TRANSFERS_PER_THREAD; i++) {
						UUID from = ids.get(rnd.nextInt(ACCOUNTS));
						UUID to = ids.get(rnd.nextInt(ACCOUNTS));
						if (from.equals(to)) {
							continue;
						}
						long amount = rnd.nextLong(1, 3_000);
						try {
							payments.transfer(new TransferCommand(from, to, Money.of(amount, USD),
									UUID.randomUUID().toString(), "load"));
							committed.incrementAndGet();
						}
						catch (InsufficientFundsException expected) {
							// A transfer larger than the source balance is correctly refused.
							rejected.incrementAndGet();
						}
					}
					return null;
				}));
			}
			for (Future<?> f : futures) {
				f.get(); // surfaces any unexpected exception as a test failure
			}
		}

		assertThat(committed.get()).isPositive();
		assertThat(committed.get() + rejected.get()).isLessThanOrEqualTo(THREADS * TRANSFERS_PER_THREAD);

		// 1) Double-entry holds globally: every signed entry sums to exactly zero.
		Long entrySum = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM ledger_entry", Long.class);
		assertThat(entrySum).isZero();

		// 2) Nothing created or destroyed: customer balances still sum to the seed total.
		long customerTotal = ids.stream().mapToLong(id -> ledger.balanceOf(id).minorUnits()).sum();
		assertThat(customerTotal).isEqualTo(initialTotal);

		// 3) No customer account dropped below its allowed minimum (0).
		for (UUID id : ids) {
			assertThat(ledger.balanceOf(id).minorUnits())
					.as("account %s must not be overdrawn", id)
					.isGreaterThanOrEqualTo(0);
		}

		// 4) The snapshot cache is consistent with the ledger re-derived from entries.
		for (UUID id : ids) {
			assertThat(ledger.balanceOf(id))
					.as("snapshot vs derived for %s", id)
					.isEqualTo(ledger.deriveBalanceFromEntries(id));
		}
	}
}
