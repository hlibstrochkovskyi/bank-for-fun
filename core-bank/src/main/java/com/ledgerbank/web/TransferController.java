package com.ledgerbank.web;

import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.payments.PaymentResult;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.TransferCommand;
import com.ledgerbank.shared.Money;
import jakarta.validation.Valid;
import java.util.Currency;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

	private final AccountService accounts;
	private final PaymentsService payments;
	private final LedgerService ledger;

	public TransferController(AccountService accounts, PaymentsService payments, LedgerService ledger) {
		this.accounts = accounts;
		this.payments = payments;
		this.ledger = ledger;
	}

	@PostMapping
	public PaymentResponse transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody TransferRequest request, @AuthenticationPrincipal Jwt jwt) {
		// Resource authorization: the source account must belong to the caller.
		accounts.requireOwnedBy(request.fromAccountId(), Principals.userId(jwt));
		Money amount = Money.ofMajor(request.amount(),
				Currency.getInstance(request.currency().trim().toUpperCase()));
		PaymentResult result = payments.transfer(new TransferCommand(
				request.fromAccountId(), request.toAccountId(), amount, idempotencyKey, request.description()));
		return new PaymentResponse(result.postingId(),
				MoneyView.from(ledger.balanceOf(request.fromAccountId())));
	}
}
