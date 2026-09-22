package io.github.rodolgiaco.oms.catalog.application.port.in;

import io.github.rodolgiaco.oms.catalog.application.ProductNotFoundException;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.util.UUID;

/** Input port for retrieving an existing {@link Product}. */
public interface GetProductUseCase {

  /**
   * Returns the product with the given identifier.
   *
   * @param productId the identifier of the product, never null
   * @return the product
   * @throws ProductNotFoundException if no product has that identifier
   */
  Product getProduct(UUID productId);
}
