package com.ledgerbank.fraud;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration for the fraud engine integration. */
@ConfigurationProperties(prefix = "ledgerbank.fraud")
public class FraudProperties {

	/** When false (the default), the no-op client allows everything. */
	private boolean enabled = false;

	/** Base URL of the Python fraud service. */
	private String baseUrl = "http://localhost:8000";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
}
