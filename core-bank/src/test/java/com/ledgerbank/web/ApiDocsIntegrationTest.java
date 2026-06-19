package com.ledgerbank.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledgerbank.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Confirms the OpenAPI docs are generated and publicly reachable (covers the "no UI yet" gap). */
@AutoConfigureMockMvc
class ApiDocsIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void apiDocsAreGeneratedAndPublic() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("ledger-bank API"))
				.andExpect(jsonPath("$.paths./api/accounts").exists())
				.andExpect(jsonPath("$.paths./api/transfers").exists());
	}

	@Test
	void swaggerUiIsServed() throws Exception {
		mvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}
}
