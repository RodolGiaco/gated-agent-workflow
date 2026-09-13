package io.github.rodolgiaco.oms.order.domain;

import java.math.BigDecimal;

/**
 * One line of an {@link Order}: how many units of a product are ordered, and at what price each.
 *
 * @param productId the identifier of the ordered product, never blank
 * @param quantity the number of units ordered, greater than zero
 * @param unitPrice the price of one unit, greater than zero
 */
public record OrderItem(String productId, int quantity, BigDecimal unitPrice) {

  /**
   * Creates an item and checks its invariants.
   *
   * @throws IllegalArgumentException if the product identifier is null or blank, the quantity is
   *     zero or negative, or the unit price is null, zero or negative
   */
  public OrderItem {
    if (productId == null || productId.isBlank()) {
      throw new IllegalArgumentException("the product identifier must not be blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException(
          "the quantity must be greater than zero, but was " + quantity);
    }
    if (unitPrice == null) {
      throw new IllegalArgumentException("the unit price must not be null");
    }
    if (unitPrice.signum() <= 0) {
      throw new IllegalArgumentException(
          "the unit price must be greater than zero, but was " + unitPrice);
    }
  }

  /**
   * Returns what this item costs in total.
   *
   * @return the quantity multiplied by the unit price
   */
  public BigDecimal subtotal() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
