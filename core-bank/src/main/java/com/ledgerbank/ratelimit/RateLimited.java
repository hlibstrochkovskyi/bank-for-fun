package com.ledgerbank.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a controller method as rate-limited under the named bucket (per user). */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

	/** The rate-limit bucket name (see ledgerbank.rate-limit.buckets). */
	String value();
}
