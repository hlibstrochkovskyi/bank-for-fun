package com.ledgerbank.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ledgerbank.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class StandingOrderApiIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder b, UUID user) {
		return b.with(jwt().jwt(j -> j.subject(user.toString())));
	}

	private UUID openAccount(UUID user) throws Exception {
		String res = mvc.perform(asUser(post("/api/accounts"), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"CHECKING\",\"currency\":\"USD\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(res, "$.id"));
	}

	@Test
	void create_thenListShowsTheStandingOrder() throws Exception {
		UUID user = UUID.randomUUID();
		UUID from = openAccount(user);
		UUID to = openAccount(user);

		mvc.perform(asUser(post("/api/accounts/{id}/standing-orders", from), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"toAccountId":"%s","amount":"50.00","currency":"USD","intervalDays":7,"description":"weekly"}"""
								.formatted(to)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.amount.minorUnits").value(5000))
				.andExpect(jsonPath("$.intervalDays").value(7))
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mvc.perform(asUser(get("/api/accounts/{id}/standing-orders", from), user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].toAccountId").value(to.toString()));
	}

	@Test
	void create_onAccountNotOwned_isForbidden() throws Exception {
		UUID owner = UUID.randomUUID();
		UUID intruder = UUID.randomUUID();
		UUID from = openAccount(owner);
		UUID to = openAccount(owner);

		mvc.perform(asUser(post("/api/accounts/{id}/standing-orders", from), intruder)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"toAccountId":"%s","amount":"50.00","currency":"USD","intervalDays":7}"""
								.formatted(to)))
				.andExpect(status().isForbidden());
	}
}
