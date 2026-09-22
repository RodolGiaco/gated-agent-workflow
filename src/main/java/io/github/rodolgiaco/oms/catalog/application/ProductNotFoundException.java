package io.github.rodolgiaco.oms.catalog.application;

import java.util.UUID;

/** Thrown when a product is asked for by an identifier that no product has. */
public class ProductNotFoundException extends RuntimeException {

  private final UUID productId;

  /**
   * Creates the exception for the identifier that was not found.
   *
   * @param productId the identifier no product has
   */
  public ProductNotFoundException(UUID productId) {
    super("no product has the identifier " + productId);
    this.productId = productId;
  }

  /**
   * Returns the identifier that was not found.
   *
   * @return the identifier
   */
  public UUID productId() {
    return productId;
  }
}
