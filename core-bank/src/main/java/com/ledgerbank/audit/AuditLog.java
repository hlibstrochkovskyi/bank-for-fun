package com.ledgerbank.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One immutable audit row: who did what to which target, when. Append-only. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreationTimestamp
	@Column(name = "occurred_at")
	private OffsetDateTime occurredAt;

	@Column(name = "actor_id")
	private UUID actorId;

	private String action;

	@Column(name = "target_type")
	private String targetType;

	@Column(name = "target_id")
	private String targetId;

	@JdbcTypeCode(SqlTypes.JSON)
	private String detail;

	protected AuditLog() {
		// for JPA
	}

	public AuditLog(UUID actorId, String action, String targetType, String targetId, String detail) {
		this.actorId = actorId;
		this.action = action;
		this.targetType = targetType;
		this.targetId = targetId;
		this.detail = detail;
	}

	public Long id() {
		return id;
	}

	public UUID actorId() {
		return actorId;
	}

	public String action() {
		return action;
	}

	public String targetType() {
		return targetType;
	}

	public String targetId() {
		return targetId;
	}
}
