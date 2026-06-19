package com.ledgerbank.accounts;

import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.shared.AccountNotFoundException;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lifecycle. Opening an account also initialises its ledger balance (a
 * zero-balance snapshot) via the {@code ledger} module's public API, so the two
 * always come into existence together.
 */
@Service
public class AccountService {

	private final AccountRepository accounts;
	private final LedgerService ledger;

	public AccountService(AccountRepository accounts, LedgerService ledger) {
		this.accounts = accounts;
		this.ledger = ledger;
	}

	/** Open a customer account with no overdraft (floor of 0). */
	@Transactional
	public Account openCustomerAccount(UUID ownerId, AccountType type, Currency currency) {
		Objects.requireNonNull(ownerId, "ownerId must not be null");
		if (type.isSystem()) {
			throw new IllegalArgumentException("system account type not allowed for a customer: " + type);
		}
		Account account = accounts.save(new Account(ownerId, type, currency));
		ledger.openBalance(account.id(), currency, 0L);
		return account;
	}

	/** Open a system account (no owner, unbounded — may go negative). */
	@Transactional
	public Account openSystemAccount(AccountType type, Currency currency) {
		if (!type.isSystem()) {
			throw new IllegalArgumentException("non-system account type not allowed for a system account: " + type);
		}
		Account account = accounts.save(new Account(null, type, currency));
		ledger.openBalance(account.id(), currency, null);
		return account;
	}

	@Transactional(readOnly = true)
	public Account require(UUID accountId) {
		return accounts.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
	}
}
