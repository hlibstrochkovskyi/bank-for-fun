package com.ledgerbank.web;

import com.ledgerbank.payments.HeldTransferService;
import com.ledgerbank.payments.PaymentResult;
import com.ledgerbank.payments.PaymentStatus;
import com.ledgerbank.payments.PaymentsService;
import com.ledgerbank.payments.ReverseCommand;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
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
	private final HeldTransferService heldTransfers;

	public AdminController(PaymentsService payments, HeldTransferService heldTransfers) {
		this.payments = payments;
		this.heldTransfers = heldTransfers;
	}

	@PostMapping("/reversals")
	public PaymentResponse reverse(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody ReversalRequest request) {
		PaymentResult result = payments.reverse(
				new ReverseCommand(request.postingId(), idempotencyKey, request.reason()));
		return PaymentResponse.from(result, null);
	}

	@PostMapping("/held-transfers/{id}/release")
	public PaymentResponse releaseHeld(@PathVariable UUID id) {
		UUID postingId = heldTransfers.release(id);
		return new PaymentResponse(PaymentStatus.COMPLETED.name(), postingId, id, null);
	}

	@PostMapping("/held-transfers/{id}/reject")
	public Map<String, String> rejectHeld(@PathVariable UUID id) {
		heldTransfers.reject(id);
		return Map.of("heldTransferId", id.toString(), "status", "REJECTED");
	}
}
