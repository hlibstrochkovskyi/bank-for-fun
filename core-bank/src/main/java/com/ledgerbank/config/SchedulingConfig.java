package com.ledgerbank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Spring's scheduling support (used by the standing-order scheduler). */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
