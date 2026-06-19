package com.ledgerbank.payments;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Wraps a money operation so a retried request never processes twice. The first
 * call runs the action and stores its result keyed by the idempotency key; replays
 * with the same key return the stored result. Reusing a key with a different
 * request payload is a conflict.
 */
@Service
public class IdempotencyService {

	private final IdempotencyRecordRepository records;
	private final ObjectMapper objectMapper;

	public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper objectMapper) {
		this.records = records;
		this.objectMapper = objectMapper;
	}

	/**
	 * Run {@code action} at most once per {@code key}. The action and the
	 * idempotency record commit together (single transaction), so a failure leaves
	 * no trace and a success is durably recorded.
	 */
	@Transactional
	public PaymentResult execute(String key, String requestHash, Supplier<PaymentResult> action) {
		Optional<IdempotencyRecord> existing = records.findById(key);
		if (existing.isPresent()) {
			IdempotencyRecord record = existing.get();
			if (!record.requestHash().equals(requestHash)) {
				throw new IdempotencyConflictException(key);
			}
			return deserialize(record.response());
		}

		PaymentResult result = action.get();
		try {
			records.saveAndFlush(
					new IdempotencyRecord(key, requestHash, serialize(result), IdempotencyStatus.COMPLETED));
		}
		catch (DataIntegrityViolationException concurrentReplay) {
			// Another request with the same key committed first; this transaction
			// (and its posting) rolls back. The client retries to get the result.
			throw new IdempotencyConflictException(key);
		}
		return result;
	}

	private String serialize(PaymentResult result) {
		try {
			return objectMapper.writeValueAsString(result);
		}
		catch (JacksonException e) {
			throw new IllegalStateException("failed to serialize payment result", e);
		}
	}

	private PaymentResult deserialize(String json) {
		try {
			return objectMapper.readValue(json, PaymentResult.class);
		}
		catch (JacksonException e) {
			throw new IllegalStateException("failed to deserialize payment result", e);
		}
	}
}
