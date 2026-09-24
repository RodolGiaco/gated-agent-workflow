package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.rodolgiaco.oms.TestcontainersConfiguration;
import io.github.rodolgiaco.oms.catalog.application.DuplicateSkuException;
import io.github.rodolgiaco.oms.catalog.application.ProductPage;
import io.github.rodolgiaco.oms.catalog.application.port.out.ProductRepository;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// Not transactional on purpose: each call to the repository commits and reads
// back in its own transaction, so a retrieved product really comes from
// PostgreSQL and not from a persistence context that still holds it. The table
// is emptied before each test so that the pages hold only what the test stored.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JpaProductRepositoryAdapterTest {

  @Autowired private ProductRepository repository;

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void emptyTheCatalog() {
    jdbc.update("DELETE FROM catalog.products");
  }

  @Test
  void persistsAProductAndRetrievesItWithAllItsValues() {
    // A scale beyond two decimals, which a fixed-scale column would have changed.
    Product product = Product.create("CHIP-1", "Chip", new BigDecimal("0.005"));

    repository.save(product);
    Product found = repository.findById(product.id()).orElseThrow();

    assertEquals(product.id(), found.id());
    assertEquals("CHIP-1", found.sku());
    assertEquals("Chip", found.name());
    assertEquals(new BigDecimal("0.005"), found.unitPrice());
  }

  @Test
  void theProductIsStoredAsARowOfTheCatalogProductsTable() {
    Product product = Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));

    repository.save(product);

    assertEquals(
        "BOOK-1",
        jdbc.queryForObject(
            "SELECT sku FROM catalog.products WHERE id = ?", String.class, product.id()));
  }

  @Test
  void findsNothingForAnUnknownIdentifier() {
    assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
  }

  @Test
  void findsAProductBySkuWithAllItsValues() {
    Product product = Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));
    repository.save(product);
    repository.save(Product.create("PEN-1", "Blue pen", BigDecimal.ONE));

    Product found = repository.findBySku("BOOK-1").orElseThrow();

    assertEquals(product.id(), found.id());
    assertEquals("BOOK-1", found.sku());
    assertEquals("Clean Code", found.name());
    assertEquals(new BigDecimal("12.50"), found.unitPrice());
  }

  @Test
  void findsNothingForAnUnknownSku() {
    repository.save(Product.create("BOOK-1", "Clean Code", BigDecimal.ONE));

    assertTrue(repository.findBySku("BOOK-2").isEmpty());
    // The SKU is compared exactly.
    assertTrue(repository.findBySku("book-1").isEmpty());
  }

  @Test
  void tellsWhetherASkuIsTaken() {
    repository.save(Product.create("BOOK-1", "Clean Code", BigDecimal.ONE));

    assertTrue(repository.existsBySku("BOOK-1"));
    assertFalse(repository.existsBySku("BOOK-2"));
    // The SKU is compared exactly.
    assertFalse(repository.existsBySku("book-1"));
  }

  // What happens when another request stores the same SKU between the check of
  // the service and this save.
  @Test
  void savingAnotherProductWithATakenSkuThrowsDuplicateSkuAndKeepsTheFirst() {
    Product first = Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));
    Product second = Product.create("BOOK-1", "Another book", new BigDecimal("9.99"));
    repository.save(first);

    DuplicateSkuException thrown =
        assertThrows(DuplicateSkuException.class, () -> repository.save(second));

    assertEquals("BOOK-1", thrown.sku());
    assertTrue(repository.findById(second.id()).isEmpty());
    assertEquals("Clean Code", repository.findById(first.id()).orElseThrow().name());
  }

  @Test
  void savingTheSameProductTwiceKeepsASingleCopyOfIt() {
    Product product = Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));

    repository.save(product);
    repository.save(product);

    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM catalog.products", Integer.class));
  }

  @Test
  void returnsTheRequestedPageOrderedBySku() {
    for (String sku : List.of("C", "A", "E", "B", "D")) {
      repository.save(Product.create(sku, "Product " + sku, BigDecimal.ONE));
    }

    ProductPage first = repository.findAll(0, 2);
    ProductPage second = repository.findAll(1, 2);
    ProductPage last = repository.findAll(2, 2);
    ProductPage beyond = repository.findAll(3, 2);

    assertEquals(List.of("A", "B"), first.products().stream().map(Product::sku).toList());
    assertEquals(List.of("C", "D"), second.products().stream().map(Product::sku).toList());
    assertEquals(List.of("E"), last.products().stream().map(Product::sku).toList());
    assertEquals(List.of(), beyond.products());
    assertEquals(1, second.page());
    assertEquals(2, second.size());
    assertEquals(5, second.totalElements());
    assertEquals(3, second.totalPages());
  }
}
