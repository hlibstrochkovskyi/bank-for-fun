package com.ledgerbank.audit;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes append-only audit entries. The acting user is resolved from the current
 * security context (the authenticated subject), or recorded as a system action
 * (null actor) when there is no authentication — e.g. scheduled jobs.
 */
@Service
public class AuditService {

	private final AuditLogRepository repository;
	private final ObjectMapper objectMapper;

	public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void log(String action, String targetType, String targetId, Object detail) {
		repository.save(new AuditLog(currentActor(), action, targetType, targetId, toJson(detail)));
	}

	private UUID currentActor() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return null;
		}
		try {
			return UUID.fromString(auth.getName());
		}
		catch (IllegalArgumentException notAUuid) {
			return null;
		}
	}

	private String toJson(Object detail) {
		if (detail == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(detail);
		}
		catch (JacksonException e) {
			return null;
		}
	}
}
