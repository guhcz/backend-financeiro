package com.personal.backend_financeiro;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Postgres container (not H2): several real bugs in this project only ever
 * surfaced against actual Postgres (native query parameter typing, SMALLINT vs INTEGER,
 * @SQLRestriction interaction with lazy loading) — an in-memory database would not have
 * caught any of them.
 *
 * The container is started manually in a static initializer and intentionally NOT
 * annotated with @Container/@Testcontainers: that JUnit extension stops the container in
 * its afterAll callback for each concrete test class, and since the field is static and
 * shared across every subclass, the second test class to run would find it already
 * stopped. This is Testcontainers' documented "singleton container" pattern — start once,
 * let the Ryuk reaper clean it up when the JVM exits.
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

	static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void registerPostgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

}
