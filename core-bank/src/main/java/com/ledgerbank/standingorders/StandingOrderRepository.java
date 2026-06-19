package com.ledgerbank.standingorders;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StandingOrderRepository extends JpaRepository<StandingOrder, UUID> {

	List<StandingOrder> findByStatusAndNextRunAtLessThanEqual(StandingOrderStatus status, OffsetDateTime at);

	List<StandingOrder> findByOwnerIdOrderByCreatedAt(UUID ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from StandingOrder o where o.id = :id")
	Optional<StandingOrder> findByIdForUpdate(@Param("id") UUID id);
}
