package com.ledgerbank.web;

import com.ledgerbank.statements.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StatementResponse(
		UUID accountId,
		OffsetDateTime from,
		OffsetDateTime to,
		MoneyView openingBalance,
		MoneyView closingBalance,
		MoneyView totalCredits,
		MoneyView totalDebits,
		List<TransactionResponse> transactions) {

	public static StatementResponse from(Statement s) {
		return new StatementResponse(s.accountId(), s.from(), s.to(),
				MoneyView.from(s.openingBalance()), MoneyView.from(s.closingBalance()),
				MoneyView.from(s.totalCredits()), MoneyView.from(s.totalDebits()),
				s.transactions().stream().map(TransactionResponse::from).toList());
	}
}
