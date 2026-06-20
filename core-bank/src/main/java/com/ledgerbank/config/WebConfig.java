package com.ledgerbank.config;

import com.ledgerbank.fraud.FraudProperties;
import com.ledgerbank.ratelimit.RateLimitInterceptor;
import com.ledgerbank.ratelimit.RateLimitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers MVC interceptors and enables configuration properties. */
@Configuration
@EnableConfigurationProperties({RateLimitProperties.class, FraudProperties.class})
class WebConfig implements WebMvcConfigurer {

	private final RateLimitInterceptor rateLimitInterceptor;

	WebConfig(RateLimitInterceptor rateLimitInterceptor) {
		this.rateLimitInterceptor = rateLimitInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
	}
}
