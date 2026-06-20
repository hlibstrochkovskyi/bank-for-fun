package com.ledgerbank.ratelimit;

/** Thrown when a caller exceeds the rate limit for a bucket. */
public class RateLimitExceededException extends RuntimeException {

	public RateLimitExceededException(String bucket) {
		super("rate limit exceeded for: " + bucket);
	}
}
