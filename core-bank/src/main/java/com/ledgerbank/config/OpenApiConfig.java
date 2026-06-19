package com.ledgerbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the generated docs / Swagger UI. Declares the bearer-JWT
 * scheme so the "Authorize" button in Swagger UI accepts a Keycloak access token.
 */
@Configuration
class OpenApiConfig {

	@Bean
	OpenAPI ledgerBankOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("ledger-bank API")
						.version("v1")
						.description("Simulated retail bank on an immutable double-entry ledger."))
				.components(new Components().addSecuritySchemes("bearer-jwt",
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
	}
}
