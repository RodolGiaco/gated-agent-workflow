package io.github.rodolgiaco.oms;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throwaway PostgreSQL in a container and points the application's data source at it.
 *
 * <p>Tests that load the full application context import this, so they never depend on a database
 * running on the machine. Every test class that imports it with the same configuration shares one
 * cached context, and therefore one container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgreSQLContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
  }
}
