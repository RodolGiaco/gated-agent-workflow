package io.github.rodolgiaco.oms.catalog.application;

import java.util.UUID;

/** Thrown when a product is asked for by an identifier or a SKU that no product has. */
public class ProductNotFoundException extends RuntimeException {

  private final UUID productId;

  private final String sku;

  /**
   * Creates the exception for the identifier that was not found.
   *
   * @param productId the identifier no product has
   */
  public ProductNotFoundException(UUID productId) {
    super("no product has the identifier " + productId);
    this.productId = productId;
    this.sku = null;
  }

  /**
   * Creates the exception for the SKU that was not found.
   *
   * @param sku the SKU no product has
   */
  public ProductNotFoundException(String sku) {
    super("no product has the SKU " + sku);
    this.productId = null;
    this.sku = sku;
  }

  /**
   * Returns the identifier that was not found.
   *
   * @return the identifier, or null when the product was asked for by its SKU
   */
  public UUID productId() {
    return productId;
  }

  /**
   * Returns the SKU that was not found.
   *
   * @return the SKU, or null when the product was asked for by its identifier
   */
  public String sku() {
    return sku;
  }
}
