package com.ledgerbank.ledger;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

	/** Re-derive an account's balance straight from the immutable entries. */
	@Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.accountId = :id")
	long sumAmountByAccountId(@Param("id") UUID id);

	/** Most-recent-first entries for an account (transaction history). */
	List<LedgerEntry> findByAccountIdOrderByIdDesc(UUID accountId, Limit limit);

	/** All entries belonging to a posting (used to build its inverse for reversal). */
	List<LedgerEntry> findByPostingId(UUID postingId);
}
