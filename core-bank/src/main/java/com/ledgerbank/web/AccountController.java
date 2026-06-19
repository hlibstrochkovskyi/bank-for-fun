package com.ledgerbank.web;

import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.payments.DepositCommand;
import com.ledgerbank.payments.PaymentResult;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.WithdrawCommand;
import com.ledgerbank.shared.Money;
import jakarta.validation.Valid;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

	private final AccountService accounts;
	private final LedgerService ledger;
	private final PaymentsService payments;

	public AccountController(AccountService accounts, LedgerService ledger, PaymentsService payments) {
		this.accounts = accounts;
		this.ledger = ledger;
		this.payments = payments;
	}

	@PostMapping
	public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		AccountType type = AccountType.valueOf(request.type().trim().toUpperCase());
		Account account = accounts.openCustomerAccount(Principals.userId(jwt), type,
				Currency.getInstance(request.currency().trim().toUpperCase()));
		AccountResponse body = AccountResponse.of(account, ledger.balanceOf(account.id()));
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping
	public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
		return accounts.listOwnedBy(Principals.userId(jwt)).stream()
				.map(account -> AccountResponse.of(account, ledger.balanceOf(account.id())))
				.toList();
	}

	@GetMapping("/{accountId}")
	public AccountResponse get(@PathVariable UUID accountId, @AuthenticationPrincipal Jwt jwt) {
		Account account = accounts.requireOwnedBy(accountId, Principals.userId(jwt));
		return AccountResponse.of(account, ledger.balanceOf(account.id()));
	}

	@GetMapping("/{accountId}/transactions")
	public List<TransactionResponse> transactions(@PathVariable UUID accountId,
			@RequestParam(defaultValue = "50") int limit, @AuthenticationPrincipal Jwt jwt) {
		accounts.requireOwnedBy(accountId, Principals.userId(jwt));
		return ledger.history(accountId, limit).stream().map(TransactionResponse::from).toList();
	}

	@PostMapping("/{accountId}/deposits")
	public PaymentResponse deposit(@PathVariable UUID accountId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody DepositRequest request, @AuthenticationPrincipal Jwt jwt) {
		accounts.requireOwnedBy(accountId, Principals.userId(jwt));
		Money amount = Money.ofMajor(request.amount(),
				Currency.getInstance(request.currency().trim().toUpperCase()));
		PaymentResult result = payments.deposit(
				new DepositCommand(accountId, amount, idempotencyKey, request.description()));
		return new PaymentResponse(result.postingId(), MoneyView.from(ledger.balanceOf(accountId)));
	}

	@PostMapping("/{accountId}/withdrawals")
	public PaymentResponse withdraw(@PathVariable UUID accountId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody WithdrawRequest request, @AuthenticationPrincipal Jwt jwt) {
		accounts.requireOwnedBy(accountId, Principals.userId(jwt));
		Money amount = Money.ofMajor(request.amount(),
				Currency.getInstance(request.currency().trim().toUpperCase()));
		PaymentResult result = payments.withdraw(
				new WithdrawCommand(accountId, amount, idempotencyKey, request.description()));
		return new PaymentResponse(result.postingId(), MoneyView.from(ledger.balanceOf(accountId)));
	}
}
