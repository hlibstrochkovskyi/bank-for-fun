package com.ledgerbank.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies {@link RateLimited} on controller methods: counts requests per user per
 * bucket in Redis and rejects with {@link RateLimitExceededException} (→ HTTP 429)
 * once the limit is exceeded.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

	private final RateLimiter limiter;
	private final RateLimitProperties properties;

	public RateLimitInterceptor(RateLimiter limiter, RateLimitProperties properties) {
		this.limiter = limiter;
		this.properties = properties;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (handler instanceof HandlerMethod method) {
			RateLimited annotation = method.getMethodAnnotation(RateLimited.class);
			if (annotation != null) {
				RateLimitProperties.Bucket bucket = properties.bucket(annotation.value());
				String key = "rl:%s:%s".formatted(annotation.value(), currentUser());
				if (!limiter.tryAcquire(key, bucket.limit(), Duration.ofSeconds(bucket.windowSeconds()))) {
					throw new RateLimitExceededException(annotation.value());
				}
			}
		}
		return true;
	}

	private String currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
	}
}
