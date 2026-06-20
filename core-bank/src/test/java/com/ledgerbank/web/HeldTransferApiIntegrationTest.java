package com.ledgerbank.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ledgerbank.AbstractIntegrationTest;
import com.ledgerbank.fraud.FraudAction;
import com.ledgerbank.fraud.FraudClient;
import com.ledgerbank.fraud.RiskDecision;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class HeldTransferApiIntegrationTest extends AbstractIntegrationTest {

	@MockitoBean
	FraudClient fraudClient;

	@Autowired
	MockMvc mvc;

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder b, UUID user) {
		return b.with(jwt().jwt(j -> j.subject(user.toString())));
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder b) {
		return b.with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_admin")));
	}

	private UUID openAccount(UUID user) throws Exception {
		String res = mvc.perform(asUser(post("/api/accounts"), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"CHECKING\",\"currency\":\"USD\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(res, "$.id"));
	}

	private UUID openFundedAccount(UUID user) throws Exception {
		UUID id = openAccount(user);
		mvc.perform(asUser(post("/api/accounts/{id}/deposits", id), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"100.00\",\"currency\":\"USD\"}"))
				.andExpect(status().isOk());
		return id;
	}

	@Test
	void flaggedTransfer_isHeld_visibleToCustomer_andReleasableByAdmin() throws Exception {
		when(fraudClient.score(any()))
				.thenReturn(new RiskDecision(0.9, FraudAction.HOLD, List.of("high amount")));
		UUID user = UUID.randomUUID();
		UUID from = openFundedAccount(user);
		UUID to = openAccount(user);

		// Transfer is accepted but held (202), with a held-transfer id.
		String res = mvc.perform(asUser(post("/api/transfers"), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fromAccountId":"%s","toAccountId":"%s","amount":"30.00","currency":"USD"}"""
								.formatted(from, to)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.status").value("HELD"))
				.andExpect(jsonPath("$.heldTransferId").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		UUID heldId = UUID.fromString(JsonPath.read(res, "$.heldTransferId"));

		// The customer can see their held transfer.
		mvc.perform(asUser(get("/api/held-transfers"), user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(heldId.toString()))
				.andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"));

		// A non-admin cannot release it.
		mvc.perform(asUser(post("/api/admin/held-transfers/{id}/release", heldId), user))
				.andExpect(status().isForbidden());

		// An admin releases it; the money then moves.
		mvc.perform(asAdmin(post("/api/admin/held-transfers/{id}/release", heldId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postingId").isNotEmpty());

		mvc.perform(asUser(get("/api/accounts/{id}", to), user))
				.andExpect(jsonPath("$.balance.minorUnits").value(3000));
	}
}
