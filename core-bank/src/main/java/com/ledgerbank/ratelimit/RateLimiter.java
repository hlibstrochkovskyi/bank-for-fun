package com.ledgerbank.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window rate limiter backed by Redis: {@code INCR} the key, set its TTL on
 * first use, and allow while the count is within the limit. Fails open if Redis is
 * unavailable (availability over strictness for a non-critical guard).
 */
@Component
public class RateLimiter {

	private final StringRedisTemplate redis;

	public RateLimiter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public boolean tryAcquire(String key, int limit, Duration window) {
		Long count = redis.opsForValue().increment(key);
		if (count == null) {
			return true; // Redis unavailable — fail open.
		}
		if (count == 1L) {
			redis.expire(key, window);
		}
		return count <= limit;
	}
}
