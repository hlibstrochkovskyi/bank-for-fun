package com.ledgerbank.web;

import com.ledgerbank.goals.SavingsGoal;
import com.ledgerbank.shared.Money;
import java.util.UUID;

public record GoalResponse(
		UUID id,
		UUID accountId,
		String accountNickname,
		String name,
		MoneyView target,
		MoneyView saved,
		double pct) {

	public static GoalResponse of(SavingsGoal goal, String accountNickname, Money saved) {
		long target = goal.targetMinor();
		double pct = target > 0 ? Math.min(100.0, (saved.minorUnits() * 100.0) / target) : 0.0;
		return new GoalResponse(goal.id(), goal.accountId(), accountNickname, goal.name(),
				MoneyView.from(Money.of(target, saved.currency())), MoneyView.from(saved), pct);
	}
}
