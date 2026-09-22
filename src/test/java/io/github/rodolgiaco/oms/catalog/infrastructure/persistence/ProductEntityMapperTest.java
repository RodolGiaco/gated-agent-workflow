package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductEntityMapperTest {

  @Test
  void aProductSurvivesTheRoundTripThroughItsEntity() {
    Product product = Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));

    Product mapped = ProductEntityMapper.toDomain(ProductEntityMapper.toEntity(product));

    assertEquals(product.id(), mapped.id());
    assertEquals(product.sku(), mapped.sku());
    assertEquals(product.name(), mapped.name());
    assertEquals(product.unitPrice(), mapped.unitPrice());
  }

  @Test
  void rejectsAStoredRowWithoutIdentifier() {
    ProductEntity entity = new ProductEntity(null, "BOOK-1", "Clean Code", BigDecimal.ONE);

    assertThrows(IllegalArgumentException.class, () -> ProductEntityMapper.toDomain(entity));
  }

  @Test
  void rejectsAStoredRowThatBreaksAnInvariant() {
    ProductEntity entity = new ProductEntity(UUID.randomUUID(), "BOOK-1", " ", BigDecimal.ONE);

    assertThrows(IllegalArgumentException.class, () -> ProductEntityMapper.toDomain(entity));
  }
}
