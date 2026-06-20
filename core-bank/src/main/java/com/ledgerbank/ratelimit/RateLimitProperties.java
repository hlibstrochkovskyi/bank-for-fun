package com.ledgerbank.ratelimit;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-bucket rate-limit configuration (e.g. the {@code money} bucket). */
@ConfigurationProperties(prefix = "ledgerbank.rate-limit")
public class RateLimitProperties {

	private Map<String, Bucket> buckets = new HashMap<>();

	public Map<String, Bucket> getBuckets() {
		return buckets;
	}

	public void setBuckets(Map<String, Bucket> buckets) {
		this.buckets = buckets;
	}

	/** Resolve a bucket's config, falling back to a sane default. */
	public Bucket bucket(String name) {
		return buckets.getOrDefault(name, new Bucket(30, 60));
	}

	public record Bucket(int limit, int windowSeconds) {
	}
}
