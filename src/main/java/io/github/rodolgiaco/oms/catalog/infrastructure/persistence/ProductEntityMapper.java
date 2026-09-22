package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import io.github.rodolgiaco.oms.catalog.domain.Product;

/** Converts between the {@link Product} model and its persistence shape, {@link ProductEntity}. */
final class ProductEntityMapper {

  private ProductEntityMapper() {}

  static ProductEntity toEntity(Product product) {
    return new ProductEntity(product.id(), product.sku(), product.name(), product.unitPrice());
  }

  /**
   * Rebuilds the model from what was stored.
   *
   * @throws IllegalArgumentException if the stored row breaks an invariant of the domain, such as a
   *     blank name
   */
  static Product toDomain(ProductEntity entity) {
    return Product.reconstitute(
        entity.getId(), entity.getSku(), entity.getName(), entity.getUnitPrice());
  }
}
