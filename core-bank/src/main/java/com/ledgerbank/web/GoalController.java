package com.ledgerbank.web;

import com.ledgerbank.accounts.Account;
import com.ledgerbank.accounts.AccountService;
import com.ledgerbank.accounts.AccountType;
import com.ledgerbank.goals.SavingsGoal;
import com.ledgerbank.goals.SavingsGoalService;
import com.ledgerbank.ledger.LedgerService;
import com.ledgerbank.shared.Money;
import jakarta.validation.Valid;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GoalController {

	private final SavingsGoalService goals;
	private final AccountService accounts;
	private final LedgerService ledger;

	public GoalController(SavingsGoalService goals, AccountService accounts, LedgerService ledger) {
		this.goals = goals;
		this.accounts = accounts;
		this.ledger = ledger;
	}

	/** Set (create or replace) the savings goal on a savings account the caller owns. */
	@PostMapping("/api/accounts/{accountId}/goal")
	public ResponseEntity<GoalResponse> set(@PathVariable UUID accountId,
			@Valid @RequestBody SetGoalRequest request, @AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = Principals.userId(jwt);
		Account account = accounts.requireOwnedBy(accountId, ownerId);
		if (account.type() != AccountType.SAVINGS) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"goals can only be set on a savings account");
		}
		Money target = Money.ofMajor(request.target(), Currency.getInstance(account.currency().getCurrencyCode()));
		SavingsGoal goal = goals.setGoal(accountId, ownerId, request.name(), target);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(GoalResponse.of(goal, account.nickname(), ledger.balanceOf(accountId)));
	}

	@GetMapping("/api/goals")
	public List<GoalResponse> list(@AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = Principals.userId(jwt);
		Map<UUID, String> nicknames = accounts.listOwnedBy(ownerId).stream()
				.collect(Collectors.toMap(Account::id,
						a -> a.nickname() == null ? "" : a.nickname(), (a, b) -> a));
		return goals.listOwnedBy(ownerId).stream()
				.map(goal -> GoalResponse.of(goal, nicknames.get(goal.accountId()),
						ledger.balanceOf(goal.accountId())))
				.toList();
	}

	@DeleteMapping("/api/accounts/{accountId}/goal")
	public ResponseEntity<Void> delete(@PathVariable UUID accountId, @AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = Principals.userId(jwt);
		accounts.requireOwnedBy(accountId, ownerId);
		goals.delete(accountId, ownerId);
		return ResponseEntity.noContent().build();
	}
}
