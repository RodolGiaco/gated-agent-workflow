package io.github.rodolgiaco.oms.catalog.application.port.in;

import io.github.rodolgiaco.oms.catalog.application.DuplicateSkuException;
import io.github.rodolgiaco.oms.catalog.application.InvalidProductException;
import io.github.rodolgiaco.oms.catalog.domain.Product;

/** Input port for adding a new {@link Product} to the catalog. */
public interface CreateProductUseCase {

  /**
   * Creates a product from the requested values and stores it.
   *
   * @param command the requested values, never null
   * @return the product as it was stored
   * @throws InvalidProductException if the requested values do not make a valid product
   * @throws DuplicateSkuException if another product already has the requested SKU
   */
  Product createProduct(CreateProductCommand command);
}
