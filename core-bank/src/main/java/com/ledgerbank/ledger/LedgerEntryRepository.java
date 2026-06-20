package com.ledgerbank.ledger;

import java.time.OffsetDateTime;
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

	/** Balance as of an instant: the sum of entries strictly before it. */
	@Query("select coalesce(sum(e.amount), 0) from LedgerEntry e "
			+ "where e.accountId = :id and e.createdAt < :before")
	long sumAmountBefore(@Param("id") UUID id, @Param("before") OffsetDateTime before);

	/** Entries within [fromInclusive, toExclusive), oldest first (for a statement). */
	List<LedgerEntry> findByAccountIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
			UUID accountId, OffsetDateTime fromInclusive, OffsetDateTime toExclusive);

	/** Most-recent-first entries for an account (transaction history). */
	List<LedgerEntry> findByAccountIdOrderByIdDesc(UUID accountId, Limit limit);

	/** All entries belonging to a posting (used to build its inverse for reversal). */
	List<LedgerEntry> findByPostingId(UUID postingId);

	/** Count of outgoing (debit) entries for an account since an instant — a velocity signal. */
	long countByAccountIdAndAmountLessThanAndCreatedAtAfter(UUID accountId, long amount, OffsetDateTime after);
}
