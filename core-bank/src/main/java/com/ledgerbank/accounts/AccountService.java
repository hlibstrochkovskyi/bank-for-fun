package com.ledgerbank.accounts;

import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.shared.AccountNotFoundException;
import com.ledgerbank.shared.events.AccountOpenedEvent;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
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
	private final ApplicationEventPublisher events;

	public AccountService(AccountRepository accounts, LedgerService ledger,
			ApplicationEventPublisher events) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.events = events;
	}

	/** Open a customer account with no overdraft (floor of 0). */
	@Transactional
	public Account openCustomerAccount(UUID ownerId, AccountType type, Currency currency) {
		return openCustomerAccount(ownerId, type, currency, null);
	}

	/** Open a customer account with an optional human nickname (no overdraft, floor of 0). */
	@Transactional
	public Account openCustomerAccount(UUID ownerId, AccountType type, Currency currency, String nickname) {
		Objects.requireNonNull(ownerId, "ownerId must not be null");
		if (type.isSystem()) {
			throw new IllegalArgumentException("system account type not allowed for a customer: " + type);
		}
		String clean = (nickname == null || nickname.isBlank()) ? defaultNickname(type) : nickname.trim();
		Account account = accounts.save(new Account(ownerId, type, currency, clean, generateAccountNumber()));
		ledger.openBalance(account.id(), currency, 0L);
		events.publishEvent(new AccountOpenedEvent(account.id(), ownerId, type.name(),
				currency.getCurrencyCode()));
		return account;
	}

	private static String defaultNickname(AccountType type) {
		return switch (type) {
			case CHECKING -> "Everyday Checking";
			case SAVINGS -> "Savings";
			default -> type.name();
		};
	}

	/** A 10-digit display account number, retried on the (astronomically rare) collision. */
	private String generateAccountNumber() {
		for (int attempt = 0; attempt < 5; attempt++) {
			String candidate = String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
			if (!accounts.existsByAccountNumber(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("could not allocate a unique account number");
	}

	/** Open a system account (no owner, unbounded — may go negative). */
	@Transactional
	public Account openSystemAccount(AccountType type, Currency currency) {
		if (!type.isSystem()) {
			throw new IllegalArgumentException("non-system account type not allowed for a system account: " + type);
		}
		Account account = accounts.save(new Account(null, type, currency));
		ledger.openBalance(account.id(), currency, null);
		events.publishEvent(new AccountOpenedEvent(account.id(), null, type.name(),
				currency.getCurrencyCode()));
		return account;
	}

	@Transactional(readOnly = true)
	public Account require(UUID accountId) {
		return accounts.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException(accountId));
	}

	@Transactional(readOnly = true)
	public List<Account> listOwnedBy(UUID ownerId) {
		return accounts.findByOwnerId(ownerId);
	}

	/**
	 * Load an account, asserting the given user owns it. Server-side resource
	 * authorization — a user may only touch their own accounts.
	 *
	 * @throws AccessDeniedException if the account is not owned by {@code ownerId}
	 */
	@Transactional(readOnly = true)
	public Account requireOwnedBy(UUID accountId, UUID ownerId) {
		Account account = require(accountId);
		if (!ownerId.equals(account.ownerId())) {
			throw new AccessDeniedException("account %s is not owned by the current user".formatted(accountId));
		}
		return account;
	}
}
