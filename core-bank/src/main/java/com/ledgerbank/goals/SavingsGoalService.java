package com.ledgerbank.goals;

import com.ledgerbank.shared.Money;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages savings goals. A goal is a target on a savings account; one per account.
 * Progress is read from the ledger elsewhere — this service owns only the target.
 */
@Service
public class SavingsGoalService {

	private final SavingsGoalRepository goals;

	public SavingsGoalService(SavingsGoalRepository goals) {
		this.goals = goals;
	}

	/** Create or replace the goal on an account. */
	@Transactional
	public SavingsGoal setGoal(UUID accountId, UUID ownerId, String name, Money target) {
		String clean = (name == null || name.isBlank()) ? "Savings goal" : name.trim();
		return goals.findByAccountId(accountId)
				.map(existing -> {
					existing.update(clean, target.minorUnits());
					return existing;
				})
				.orElseGet(() -> goals.save(new SavingsGoal(accountId, ownerId, clean,
						target.minorUnits(), target.currency().getCurrencyCode())));
	}

	@Transactional(readOnly = true)
	public List<SavingsGoal> listOwnedBy(UUID ownerId) {
		return goals.findByOwnerIdOrderByCreatedAtDesc(ownerId);
	}

	@Transactional
	public void delete(UUID accountId, UUID ownerId) {
		goals.findByAccountId(accountId)
				.filter(g -> g.ownerId().equals(ownerId))
				.ifPresent(goals::delete);
	}
}
