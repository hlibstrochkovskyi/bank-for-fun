package com.ledgerbank.web;

import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.shared.Money;
import com.ledgerbank.standingorders.CreateStandingOrderCommand;
import com.ledgerbank.standingorders.StandingOrder;
import com.ledgerbank.standingorders.StandingOrderService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/standing-orders")
public class StandingOrderController {

	private final AccountService accounts;
	private final StandingOrderService standingOrders;

	public StandingOrderController(AccountService accounts, StandingOrderService standingOrders) {
		this.accounts = accounts;
		this.standingOrders = standingOrders;
	}

	@PostMapping
	public ResponseEntity<StandingOrderResponse> create(@PathVariable UUID accountId,
			@Valid @RequestBody CreateStandingOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID owner = Principals.userId(jwt);
		accounts.requireOwnedBy(accountId, owner);
		Money amount = Money.ofMajor(request.amount(),
				Currency.getInstance(request.currency().trim().toUpperCase()));
		OffsetDateTime firstRun = request.firstRunAt() != null
				? request.firstRunAt() : OffsetDateTime.now(ZoneOffset.UTC);
		StandingOrder order = standingOrders.create(new CreateStandingOrderCommand(
				owner, accountId, request.toAccountId(), amount, request.intervalDays(),
				firstRun, request.description()));
		return ResponseEntity.status(HttpStatus.CREATED).body(StandingOrderResponse.from(order));
	}

	@GetMapping
	public List<StandingOrderResponse> list(@PathVariable UUID accountId,
			@AuthenticationPrincipal Jwt jwt) {
		UUID owner = Principals.userId(jwt);
		accounts.requireOwnedBy(accountId, owner);
		return standingOrders.listOwnedBy(owner).stream()
				.filter(order -> order.fromAccountId().equals(accountId))
				.map(StandingOrderResponse::from)
				.toList();
	}
}
