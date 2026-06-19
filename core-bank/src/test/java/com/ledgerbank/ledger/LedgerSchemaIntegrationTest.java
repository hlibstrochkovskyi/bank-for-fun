package com.ledgerbank.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ledgerbank.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves the database itself enforces the double-entry invariant via the deferred
 * balanced-posting constraint trigger — independent of application code.
 */
class LedgerSchemaIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TransactionTemplate tx;

	private UUID openAccount(String currency) {
		UUID id = UUID.randomUUID();
		jdbc.update(
				"INSERT INTO account (id, owner_id, type, currency) VALUES (?, ?, 'CHECKING', ?)",
				id, UUID.randomUUID(), currency);
		return id;
	}

	private UUID newPosting(String key) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO posting (id, type, idempotency_key) VALUES (?, 'TRANSFER', ?)", id, key);
		return id;
	}

	@Test
	void acceptsBalancedPosting() {
		UUID from = openAccount("USD");
		UUID to = openAccount("USD");

		assertThatCode(() -> tx.executeWithoutResult(s -> {
			UUID posting = newPosting("balanced-" + UUID.randomUUID());
			jdbc.update("INSERT INTO ledger_entry (posting_id, account_id, amount, currency) VALUES (?, ?, ?, 'USD')",
					posting, from, -500);
			jdbc.update("INSERT INTO ledger_entry (posting_id, account_id, amount, currency) VALUES (?, ?, ?, 'USD')",
					posting, to, 500);
		})).doesNotThrowAnyException();

		Long total = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM ledger_entry", Long.class);
		assertThat(total).isZero();
	}

	@Test
	void rejectsUnbalancedPostingAtCommit() {
		UUID from = openAccount("USD");
		UUID to = openAccount("USD");

		assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
			UUID posting = newPosting("unbalanced-" + UUID.randomUUID());
			jdbc.update("INSERT INTO ledger_entry (posting_id, account_id, amount, currency) VALUES (?, ?, ?, 'USD')",
					posting, from, -500);
			jdbc.update("INSERT INTO ledger_entry (posting_id, account_id, amount, currency) VALUES (?, ?, ?, 'USD')",
					posting, to, 400); // does not offset -500
		})).hasMessageContaining("unbalanced");
	}

	@Test
	void rejectsSingleEntryPostingAtCommit() {
		UUID acct = openAccount("USD");

		assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
			UUID posting = newPosting("single-" + UUID.randomUUID());
			jdbc.update("INSERT INTO ledger_entry (posting_id, account_id, amount, currency) VALUES (?, ?, ?, 'USD')",
					posting, acct, 500);
		})).hasMessageContaining("at least two entries");
	}

	@Test
	void rejectsZeroAmountEntry() {
		UUID acct = openAccount("USD");

		assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
			UUID posting = newPosting("zero-" + UUID.randomUUID());
			jdbc.update("INSERT INTO ledger_entry (posting_id, account_id, amount, currency) VALUES (?, ?, ?, 'USD')",
					posting, acct, 0);
		})).isInstanceOf(Exception.class);
	}
}
