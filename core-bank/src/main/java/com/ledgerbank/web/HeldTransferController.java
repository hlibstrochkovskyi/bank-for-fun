package com.ledgerbank.web;

import com.ledgerbank.payments.HeldTransferService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A customer's view of their own transfers held for fraud review. */
@RestController
@RequestMapping("/api/held-transfers")
public class HeldTransferController {

	private final HeldTransferService heldTransfers;

	public HeldTransferController(HeldTransferService heldTransfers) {
		this.heldTransfers = heldTransfers;
	}

	@GetMapping
	public List<HeldTransferResponse> mine(@AuthenticationPrincipal Jwt jwt) {
		return heldTransfers.listForOwner(Principals.userId(jwt)).stream()
				.map(HeldTransferResponse::from)
				.toList();
	}
}
