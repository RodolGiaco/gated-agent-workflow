package io.github.rodolgiaco.oms.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTest {

  private static final BigDecimal PRICE = new BigDecimal("12.50");

  @Test
  void createsAProductWithTheGivenValues() {
    Product product = Product.create("BOOK-1", "Clean Code", PRICE);

    assertNotNull(product.id());
    assertEquals("BOOK-1", product.sku());
    assertEquals("Clean Code", product.name());
    assertEquals(PRICE, product.unitPrice());
  }

  @Test
  void everyNewProductGetsItsOwnIdentifier() {
    assertNotEquals(
        Product.create("BOOK-1", "Clean Code", PRICE).id(),
        Product.create("BOOK-1", "Clean Code", PRICE).id());
  }

  @Test
  void acceptsTheSmallestPositiveUnitPrice() {
    assertEquals(
        new BigDecimal("0.005"),
        Product.create("CHIP-1", "Chip", new BigDecimal("0.005")).unitPrice());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
  void rejectsANullOrBlankSku(String sku) {
    assertThrows(IllegalArgumentException.class, () -> Product.create(sku, "Clean Code", PRICE));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
  void rejectsANullOrBlankName(String name) {
    assertThrows(IllegalArgumentException.class, () -> Product.create("BOOK-1", name, PRICE));
  }

  @Test
  void rejectsAZeroUnitPrice() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.create("BOOK-1", "Clean Code", BigDecimal.ZERO));
    // A zero with a scale is still zero.
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.create("BOOK-1", "Clean Code", new BigDecimal("0.00")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.01", "-1", "-12.50"})
  void rejectsANegativeUnitPrice(String unitPrice) {
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.create("BOOK-1", "Clean Code", new BigDecimal(unitPrice)));
  }

  @Test
  void rejectsANullUnitPrice() {
    assertThrows(
        IllegalArgumentException.class, () -> Product.create("BOOK-1", "Clean Code", null));
  }

  @Test
  void reconstitutingKeepsTheGivenIdentifierAndValues() {
    UUID id = UUID.randomUUID();

    Product product = Product.reconstitute(id, "BOOK-1", "Clean Code", PRICE);

    assertEquals(id, product.id());
    assertEquals("BOOK-1", product.sku());
    assertEquals("Clean Code", product.name());
    assertEquals(PRICE, product.unitPrice());
  }

  @Test
  void reconstitutingRejectsANullIdentifier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.reconstitute(null, "BOOK-1", "Clean Code", PRICE));
  }

  @Test
  void reconstitutingEnforcesTheSameInvariantsAsCreating() {
    UUID id = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class, () -> Product.reconstitute(id, " ", "Clean Code", PRICE));
    assertThrows(
        IllegalArgumentException.class, () -> Product.reconstitute(id, "BOOK-1", " ", PRICE));
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.reconstitute(id, "BOOK-1", "Clean Code", BigDecimal.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.reconstitute(id, "BOOK-1", "Clean Code", new BigDecimal("-1")));
    assertThrows(
        IllegalArgumentException.class,
        () -> Product.reconstitute(id, "BOOK-1", "Clean Code", null));
  }
}
