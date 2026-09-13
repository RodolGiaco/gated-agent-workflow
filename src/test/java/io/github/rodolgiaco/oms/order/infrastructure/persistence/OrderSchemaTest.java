package io.github.rodolgiaco.oms.order.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.rodolgiaco.oms.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderSchemaTest {

  @Autowired private Flyway flyway;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbc;

  @Test
  void flywayAppliedTheVersionedMigrationThatCreatesTheOrderTables() {
    List<MigrationInfo> applied = Arrays.asList(flyway.info().applied());

    assertEquals(
        List.of("1"), applied.stream().map(info -> info.getVersion().getVersion()).toList());
    assertEquals(MigrationState.SUCCESS, applied.get(0).getState());
    assertEquals("create orders and order items", applied.get(0).getDescription());
  }

  @Test
  void hibernateOnlyValidatesTheSchema() {
    assertEquals("validate", entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto"));
  }

  // If Hibernate generated any part of the schema, a table it named would show
  // up here next to the ones the migration created.
  @Test
  void theSchemaHoldsOnlyTheTablesTheMigrationCreated() {
    Set<String> tables =
        jdbc
            .queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class)
            .stream()
            .collect(Collectors.toSet());

    assertEquals(Set.of("flyway_schema_history", "orders", "order_items"), tables);
  }
}
