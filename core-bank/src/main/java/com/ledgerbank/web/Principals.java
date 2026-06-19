package com.ledgerbank.web;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Maps an authenticated JWT to the application's user id (the token subject). */
final class Principals {

	private Principals() {
	}

	static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
