package com.ledgerbank.payments;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeldTransferRepository extends JpaRepository<HeldTransfer, UUID> {

	List<HeldTransfer> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	List<HeldTransfer> findByStatusOrderByCreatedAtDesc(HeldTransferStatus status);
}
