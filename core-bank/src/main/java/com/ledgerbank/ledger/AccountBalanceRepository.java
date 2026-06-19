package com.ledgerbank.ledger;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {

	/**
	 * Load a balance row with a pessimistic write lock ({@code SELECT ... FOR UPDATE}).
	 * Callers acquire these in a deterministic order to make deadlocks impossible.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from AccountBalance b where b.accountId = :id")
	Optional<AccountBalance> findByIdForUpdate(@Param("id") UUID id);
}
