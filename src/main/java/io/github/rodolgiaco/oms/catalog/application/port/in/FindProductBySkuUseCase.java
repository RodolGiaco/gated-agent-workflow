package io.github.rodolgiaco.oms.catalog.application.port.in;

import io.github.rodolgiaco.oms.catalog.application.ProductNotFoundException;
import io.github.rodolgiaco.oms.catalog.domain.Product;

/** Input port for retrieving an existing {@link Product} by its SKU. */
public interface FindProductBySkuUseCase {

  /**
   * Returns the product with the given SKU.
   *
   * @param sku the SKU of the product, compared exactly, never null
   * @return the product
   * @throws ProductNotFoundException if no product has that SKU
   */
  Product findProductBySku(String sku);
}
