package io.github.rodolgiaco.oms.catalog.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A product offered in the catalog, and the model that guards its invariants.
 *
 * <p>A product always has an identifier, a SKU and a name that are not blank, and a unit price
 * greater than zero. None of them changes once the product is created.
 */
public final class Product {

  private final UUID id;

  private final String sku;

  private final String name;

  private final BigDecimal unitPrice;

  private Product(UUID id, String sku, String name, BigDecimal unitPrice) {
    if (sku == null || sku.isBlank()) {
      throw new IllegalArgumentException("the SKU must not be blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("the name must not be blank");
    }
    if (unitPrice == null) {
      throw new IllegalArgumentException("the unit price must not be null");
    }
    if (unitPrice.signum() <= 0) {
      throw new IllegalArgumentException(
          "the unit price must be greater than zero, but was " + unitPrice);
    }
    this.id = id;
    this.sku = sku;
    this.name = name;
    this.unitPrice = unitPrice;
  }

  /**
   * Creates a new product with a fresh identifier.
   *
   * @param sku the stock keeping unit that identifies the product to people, never blank
   * @param name the name of the product, never blank
   * @param unitPrice the price of one unit, greater than zero
   * @return the new product
   * @throws IllegalArgumentException if the SKU or the name is null or blank, or the unit price is
   *     null, zero or negative
   */
  public static Product create(String sku, String name, BigDecimal unitPrice) {
    return new Product(UUID.randomUUID(), sku, name, unitPrice);
  }

  /**
   * Rebuilds a product that already exists, such as one read back from storage.
   *
   * <p>Unlike {@link #create(String, String, BigDecimal)}, this keeps the given identifier instead
   * of assigning a new one. The same invariants hold for the other values.
   *
   * @param id the identifier the product already has
   * @param sku the stock keeping unit of the product, never blank
   * @param name the name of the product, never blank
   * @param unitPrice the price of one unit, greater than zero
   * @return the rebuilt product
   * @throws IllegalArgumentException if the identifier is null, the SKU or the name is null or
   *     blank, or the unit price is null, zero or negative
   */
  public static Product reconstitute(UUID id, String sku, String name, BigDecimal unitPrice) {
    if (id == null) {
      throw new IllegalArgumentException("a product must have an identifier");
    }
    return new Product(id, sku, name, unitPrice);
  }

  /**
   * Returns the identifier of this product.
   *
   * @return the identifier, never null
   */
  public UUID id() {
    return id;
  }

  /**
   * Returns the stock keeping unit of this product.
   *
   * @return the SKU, never blank
   */
  public String sku() {
    return sku;
  }

  /**
   * Returns the name of this product.
   *
   * @return the name, never blank
   */
  public String name() {
    return name;
  }

  /**
   * Returns the price of one unit of this product.
   *
   * @return the unit price, greater than zero
   */
  public BigDecimal unitPrice() {
    return unitPrice;
  }
}
