package com.ledgerbank.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ledgerbank.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"ledgerbank.rate-limit.buckets.money.limit=3",
		"ledgerbank.rate-limit.buckets.money.window-seconds=60"
})
class RateLimitApiIntegrationTest extends AbstractIntegrationTest {

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
	void moneyEndpoint_rejectsOnceLimitExceeded() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openAccount(user);

		// Limit is 3 deposits per window for this user; the 4th is throttled.
		for (int i = 0; i < 3; i++) {
			mvc.perform(asUser(post("/api/accounts/{id}/deposits", account), user)
							.header("Idempotency-Key", UUID.randomUUID().toString())
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\":\"1.00\",\"currency\":\"USD\"}"))
					.andExpect(status().isOk());
		}

		mvc.perform(asUser(post("/api/accounts/{id}/deposits", account), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"1.00\",\"currency\":\"USD\"}"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void limitIsPerUser() throws Exception {
		UUID alice = UUID.randomUUID();
		UUID bob = UUID.randomUUID();
		UUID aliceAccount = openAccount(alice);
		UUID bobAccount = openAccount(bob);

		for (int i = 0; i < 3; i++) {
			mvc.perform(asUser(post("/api/accounts/{id}/deposits", aliceAccount), alice)
							.header("Idempotency-Key", UUID.randomUUID().toString())
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\":\"1.00\",\"currency\":\"USD\"}"))
					.andExpect(status().isOk());
		}

		// Bob is unaffected by Alice's usage.
		mvc.perform(asUser(post("/api/accounts/{id}/deposits", bobAccount), bob)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"1.00\",\"currency\":\"USD\"}"))
				.andExpect(status().isOk());
	}
}
