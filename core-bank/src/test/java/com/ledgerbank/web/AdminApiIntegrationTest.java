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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class AdminApiIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	private MockHttpServletRequestBuilder asCustomer(MockHttpServletRequestBuilder b, UUID user) {
		return b.with(jwt().jwt(j -> j.subject(user.toString())));
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder b, UUID user) {
		return b.with(jwt().jwt(j -> j.subject(user.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_admin")));
	}

	private UUID openAccountAndDeposit(UUID user) throws Exception {
		String res = mvc.perform(asCustomer(post("/api/accounts"), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"CHECKING\",\"currency\":\"USD\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID account = UUID.fromString(JsonPath.read(res, "$.id"));
		mvc.perform(asCustomer(post("/api/accounts/{id}/deposits", account), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"100.00\",\"currency\":\"USD\"}"))
				.andExpect(status().isOk());
		return account;
	}

	private UUID lastPostingId(UUID user, UUID account) throws Exception {
		String res = mvc.perform(asCustomer(get("/api/accounts/{id}/transactions", account), user))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(res, "$[0].postingId"));
	}

	@Test
	void admin_canReversePosting() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openAccountAndDeposit(user);
		UUID posting = lastPostingId(user, account);

		mvc.perform(asAdmin(post("/api/admin/reversals"), UUID.randomUUID())
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postingId\":\"%s\",\"reason\":\"test\"}".formatted(posting)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postingId").isNotEmpty());

		mvc.perform(asCustomer(get("/api/accounts/{id}", account), user))
				.andExpect(jsonPath("$.balance.minorUnits").value(0));
	}

	@Test
	void nonAdmin_cannotReverse() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openAccountAndDeposit(user);
		UUID posting = lastPostingId(user, account);

		mvc.perform(asCustomer(post("/api/admin/reversals"), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"postingId\":\"%s\"}".formatted(posting)))
				.andExpect(status().isForbidden());
	}
}
