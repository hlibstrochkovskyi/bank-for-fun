package com.ledgerbank;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Base class for integration tests. Boots the full Spring context against a real
 * PostgreSQL container (Testcontainers). All subclasses share the same context
 * configuration, so Spring caches one context and reuses a single container across
 * the suite.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
}
