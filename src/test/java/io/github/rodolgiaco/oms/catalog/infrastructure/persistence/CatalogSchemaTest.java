package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.rodolgiaco.oms.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogSchemaTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void flywayAppliedTheVersionedMigrationThatCreatesTheProductsTable() {
    List<Map<String, Object>> applied =
        jdbc.queryForList(
            "SELECT version, description, success FROM catalog.flyway_schema_history"
                + " WHERE type = 'SQL' ORDER BY installed_rank");

    assertEquals(
        List.of(Map.of("version", "1", "description", "create products", "success", true)),
        applied);
  }

  // If Hibernate generated any part of the schema, a table it named would show
  // up here next to the ones the migration created.
  @Test
  void theCatalogSchemaHoldsOnlyTheTablesTheMigrationCreated() {
    Set<String> tables =
        jdbc
            .queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'catalog'",
                String.class)
            .stream()
            .collect(Collectors.toSet());

    assertEquals(Set.of("flyway_schema_history", "products"), tables);
  }

  @Test
  void theSkuIsUniqueInTheDatabase() {
    assertEquals(
        List.of("sku"),
        jdbc.queryForList(
            "SELECT column_name FROM information_schema.constraint_column_usage"
                + " WHERE table_schema = 'catalog' AND constraint_name = ?",
            String.class,
            JpaProductRepositoryAdapter.SKU_CONSTRAINT));
  }
}
