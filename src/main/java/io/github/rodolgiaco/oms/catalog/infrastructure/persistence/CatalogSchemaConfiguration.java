package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates and upgrades the {@code catalog} schema with Flyway when the application starts.
 *
 * <p>The orders tables come from the Flyway that Spring Boot configures over {@code db/migration},
 * whose history lives in the default schema. The catalog uses the same mechanism, versioned SQL
 * migrations that Flyway applies before Hibernate validates the entities, but keeps its migrations
 * under {@code db/catalog} and its history in its own schema. Each module thus owns its tables and
 * their history, and adding the catalog leaves the orders schema and its history as they were.
 *
 * <p>A {@link Configuration} because it only declares beans, all singletons. It exposes no bean of
 * type {@link Flyway}: one would make Spring Boot skip the Flyway it configures for the orders.
 */
@Configuration(proxyBeanMethods = false)
class CatalogSchemaConfiguration {

  /** The database schema that holds the catalog tables and their migration history. */
  static final String SCHEMA = "catalog";

  @Bean
  CatalogSchemaMigration catalogSchemaMigration(DataSource dataSource) {
    return new CatalogSchemaMigration(
        Flyway.configure()
            .dataSource(dataSource)
            .schemas(SCHEMA)
            .locations("classpath:db/catalog")
            .load());
  }

  // Hibernate validates the entities against the tables when the entity
  // manager factory starts, so the catalog has to be migrated before that.
  // Static because it acts on bean definitions, before this class exists.
  @Bean
  static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnCatalogSchema() {
    return new EntityManagerFactoryDependsOnPostProcessor(CatalogSchemaMigration.class);
  }

  /** Applies the pending catalog migrations when Spring initialises it. */
  static final class CatalogSchemaMigration implements InitializingBean {

    private final Flyway flyway;

    CatalogSchemaMigration(Flyway flyway) {
      this.flyway = flyway;
    }

    @Override
    public void afterPropertiesSet() {
      flyway.migrate();
    }
  }
}
