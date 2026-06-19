package com.ledgerbank.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

	boolean existsByIdempotencyKey(String idempotencyKey);

	boolean existsByReversesPostingId(UUID reversesPostingId);
}
