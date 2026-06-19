package com.ledgerbank.payments;

import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountRepository;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the system clearing account for a currency — the counterparty that
 * represents the outside world for deposits and withdrawals, so external money
 * movements still balance. Created on first use per currency.
 */
@Service
public class SystemAccountService {

	private final AccountRepository accounts;
	private final AccountService accountService;

	public SystemAccountService(AccountRepository accounts, AccountService accountService) {
		this.accounts = accounts;
		this.accountService = accountService;
	}

	@Transactional
	public UUID clearingAccountFor(Currency currency) {
		return accounts
				.findFirstByTypeAndCurrencyOrderByCreatedAt(AccountType.SYSTEM_CLEARING, currency.getCurrencyCode())
				.map(Account::id)
				.orElseGet(() -> accountService.openSystemAccount(AccountType.SYSTEM_CLEARING, currency).id());
	}
}
