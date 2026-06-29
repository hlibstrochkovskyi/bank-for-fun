package com.ledgerbank.goals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {

	List<SavingsGoal> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	Optional<SavingsGoal> findByAccountId(UUID accountId);
}
