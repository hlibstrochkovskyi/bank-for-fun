package com.ledgerbank.cards;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {

	List<Card> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<Card> findByAccountId(UUID accountId);
}
