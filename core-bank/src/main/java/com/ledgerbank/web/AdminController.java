package com.ledgerbank.web;

import com.ledgerbank.payments.PaymentResult;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.ReverseCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative operations. Guarded by the {@code admin} realm role (see
 * SecurityConfig). Reversals are corrections, hence an admin capability rather than
 * a customer one.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final PaymentsService payments;

	public AdminController(PaymentsService payments) {
		this.payments = payments;
	}

	@PostMapping("/reversals")
	public PaymentResponse reverse(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody ReversalRequest request) {
		PaymentResult result = payments.reverse(
				new ReverseCommand(request.postingId(), idempotencyKey, request.reason()));
		return new PaymentResponse(result.postingId(), null);
	}
}
