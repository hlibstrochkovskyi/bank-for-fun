package com.ledgerbank.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Exercises the real HTTP call to the fraud service against an in-process stub
 * server (the held-flow tests mock {@link FraudClient}, so they would not catch a
 * malformed request body — which is exactly the bug live verification surfaced).
 */
class RestFraudClientTest {

	@Test
	void sendsSnakeCaseJsonBody_andParsesHold() throws IOException {
		AtomicReference<String> received = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/score", exchange -> {
			received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] body = "{\"score\":0.8,\"action\":\"HOLD\",\"reasons\":[\"high amount\"]}"
					.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		try {
			RestFraudClient client = clientFor(server);

			RiskDecision decision = client.score(new FraudCheckRequest(
					1_500_000, "USD", UUID.randomUUID(), UUID.randomUUID(), true, 3));

			assertThat(decision.action()).isEqualTo(FraudAction.HOLD);
			assertThat(decision.reasons()).containsExactly("high amount");
			// The request body was actually serialized (snake_case, as FastAPI expects).
			assertThat(received.get())
					.contains("\"amount_minor\":1500000")
					.contains("\"currency\":\"USD\"")
					.contains("\"new_payee\":true")
					.contains("\"recent_transfer_count\":3");
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void failsOpen_whenServiceErrors() throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/score", exchange -> {
			exchange.sendResponseHeaders(500, -1);
			exchange.close();
		});
		server.start();
		try {
			RiskDecision decision = clientFor(server).score(new FraudCheckRequest(
					100, "USD", UUID.randomUUID(), UUID.randomUUID(), false, 0));

			assertThat(decision.action()).isEqualTo(FraudAction.ALLOW);
		}
		finally {
			server.stop(0);
		}
	}

	private RestFraudClient clientFor(HttpServer server) {
		FraudProperties properties = new FraudProperties();
		properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
		return new RestFraudClient(properties);
	}
}
