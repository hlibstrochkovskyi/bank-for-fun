package com.ledgerbank.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

	boolean existsByIdempotencyKey(String idempotencyKey);

	boolean existsByReversesPostingId(UUID reversesPostingId);

	/** Whether a transfer has previously moved money from {@code from} to {@code to}. */
	@Query("""
			select count(p) > 0 from Posting p
			where p.type = com.ledgerbank.ledger.PostingType.TRANSFER
			  and exists (select e1 from LedgerEntry e1 where e1.postingId = p.id and e1.accountId = :from and e1.amount < 0)
			  and exists (select e2 from LedgerEntry e2 where e2.postingId = p.id and e2.accountId = :to and e2.amount > 0)
			""")
	boolean existsTransferBetween(@Param("from") UUID from, @Param("to") UUID to);
}
