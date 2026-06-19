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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@AutoConfigureMockMvc
class ApiIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	MockMvc mvc;

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, UUID user) {
		return builder.with(jwt().jwt(jwtBuilder -> jwtBuilder.subject(user.toString())));
	}

	private UUID openCheckingAccount(UUID user) throws Exception {
		String response = mvc.perform(asUser(post("/api/accounts"), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"CHECKING\",\"currency\":\"USD\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}

	private void deposit(UUID user, UUID account, String amount, String key) throws Exception {
		mvc.perform(asUser(post("/api/accounts/{id}/deposits", account), user)
						.header("Idempotency-Key", key)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"%s\",\"currency\":\"USD\"}".formatted(amount)))
				.andExpect(status().isOk());
	}

	@Test
	void openAccount_appearsInListWithZeroBalance() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openCheckingAccount(user);

		mvc.perform(asUser(get("/api/accounts"), user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(account.toString()))
				.andExpect(jsonPath("$[0].balance.amount").value("0.00"))
				.andExpect(jsonPath("$[0].balance.minorUnits").value(0));
	}

	@Test
	void deposit_thenBalanceReflectsIt() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openCheckingAccount(user);

		mvc.perform(asUser(post("/api/accounts/{id}/deposits", account), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"100.00\",\"currency\":\"USD\",\"description\":\"paycheck\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.postingId").isNotEmpty())
				.andExpect(jsonPath("$.balanceAfter.amount").value("100.00"))
				.andExpect(jsonPath("$.balanceAfter.minorUnits").value(10000));

		mvc.perform(asUser(get("/api/accounts/{id}", account), user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance.minorUnits").value(10000));
	}

	@Test
	void transfer_movesMoneyAndShowsInHistory() throws Exception {
		UUID user = UUID.randomUUID();
		UUID from = openCheckingAccount(user);
		UUID to = openCheckingAccount(user);
		deposit(user, from, "100.00", UUID.randomUUID().toString());

		mvc.perform(asUser(post("/api/transfers"), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fromAccountId":"%s","toAccountId":"%s","amount":"30.00","currency":"USD"}"""
								.formatted(from, to)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balanceAfter.minorUnits").value(7000));

		mvc.perform(asUser(get("/api/accounts/{id}", to), user))
				.andExpect(jsonPath("$.balance.minorUnits").value(3000));

		mvc.perform(asUser(get("/api/accounts/{id}/transactions", from), user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].type").value("TRANSFER"))
				.andExpect(jsonPath("$[0].amount.minorUnits").value(-3000));
	}

	@Test
	void deposit_isIdempotent_replayWithSameKeyDoesNotDoubleCharge() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openCheckingAccount(user);
		String key = UUID.randomUUID().toString();

		deposit(user, account, "50.00", key);
		deposit(user, account, "50.00", key); // replay

		mvc.perform(asUser(get("/api/accounts/{id}", account), user))
				.andExpect(jsonPath("$.balance.minorUnits").value(5000));
	}

	@Test
	void transferBeyondBalance_isUnprocessable() throws Exception {
		UUID user = UUID.randomUUID();
		UUID from = openCheckingAccount(user);
		UUID to = openCheckingAccount(user);
		deposit(user, from, "10.00", UUID.randomUUID().toString());

		mvc.perform(asUser(post("/api/transfers"), user)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fromAccountId":"%s","toAccountId":"%s","amount":"99.00","currency":"USD"}"""
								.formatted(from, to)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.title").value("Insufficient funds"));
	}

	@Test
	void accessingAnotherUsersAccount_isForbidden() throws Exception {
		UUID owner = UUID.randomUUID();
		UUID intruder = UUID.randomUUID();
		UUID account = openCheckingAccount(owner);

		mvc.perform(asUser(get("/api/accounts/{id}", account), intruder))
				.andExpect(status().isForbidden());
	}

	@Test
	void transferFromAccountNotOwned_isForbidden() throws Exception {
		UUID owner = UUID.randomUUID();
		UUID intruder = UUID.randomUUID();
		UUID from = openCheckingAccount(owner);
		UUID to = openCheckingAccount(intruder);

		mvc.perform(asUser(post("/api/transfers"), intruder)
						.header("Idempotency-Key", UUID.randomUUID().toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"fromAccountId":"%s","toAccountId":"%s","amount":"1.00","currency":"USD"}"""
								.formatted(from, to)))
				.andExpect(status().isForbidden());
	}

	@Test
	void unauthenticatedRequest_isUnauthorized() throws Exception {
		mvc.perform(get("/api/accounts"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deposit_withoutIdempotencyKey_isBadRequest() throws Exception {
		UUID user = UUID.randomUUID();
		UUID account = openCheckingAccount(user);

		mvc.perform(asUser(post("/api/accounts/{id}/deposits", account), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":\"10.00\",\"currency\":\"USD\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void openAccount_withInvalidBody_isBadRequest() throws Exception {
		UUID user = UUID.randomUUID();

		mvc.perform(asUser(post("/api/accounts"), user)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"CHECKING\"}"))
				.andExpect(status().isBadRequest());
	}
}
