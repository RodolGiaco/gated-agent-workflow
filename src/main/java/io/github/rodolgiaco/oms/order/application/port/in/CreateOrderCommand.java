package io.github.rodolgiaco.oms.order.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * What a caller asks for when it creates an order.
 *
 * <p>The values are not checked here: the order domain decides whether they make a valid order.
 *
 * @param items the lines the order should contain
 */
public record CreateOrderCommand(List<Item> items) {

  /**
   * One requested line of the order.
   *
   * @param productId the identifier of the product
   * @param quantity the number of units
   * @param unitPrice the price of one unit
   */
  public record Item(String productId, int quantity, BigDecimal unitPrice) {}
}
