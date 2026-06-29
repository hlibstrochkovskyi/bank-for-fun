package com.ledgerbank.enrichment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingEnrichmentRepository extends JpaRepository<PostingEnrichment, UUID> {

	List<PostingEnrichment> findByPostingIdIn(Collection<UUID> postingIds);
}
