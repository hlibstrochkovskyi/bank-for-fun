package com.ledgerbank;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests. Boots the full Spring context against a real
 * PostgreSQL container (Testcontainers). All subclasses share the same context
 * configuration, so Spring caches one context and reuses a single container across
 * the suite. The background scheduler is disabled so tests drive scheduled work
 * (e.g. standing orders) explicitly and deterministically.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "ledgerbank.scheduler.enabled=false")
public abstract class AbstractIntegrationTest {
}
