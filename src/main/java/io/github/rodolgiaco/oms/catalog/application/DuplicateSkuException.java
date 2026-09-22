package io.github.rodolgiaco.oms.catalog.application;

/** Thrown when a product is created with a SKU that another product already has. */
public class DuplicateSkuException extends RuntimeException {

  private final String sku;

  /**
   * Creates the exception for the SKU that is already taken.
   *
   * @param sku the SKU another product already has
   */
  public DuplicateSkuException(String sku) {
    super("a product with the SKU " + sku + " already exists");
    this.sku = sku;
  }

  /**
   * Returns the SKU that is already taken.
   *
   * @return the SKU
   */
  public String sku() {
    return sku;
  }
}
