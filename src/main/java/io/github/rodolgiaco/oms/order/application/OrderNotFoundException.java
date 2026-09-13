package io.github.rodolgiaco.oms.order.application;

import java.util.UUID;

/** Thrown when an order is asked for by an identifier that no order has. */
public class OrderNotFoundException extends RuntimeException {

  private final UUID orderId;

  /**
   * Creates the exception for the identifier that was not found.
   *
   * @param orderId the identifier no order has
   */
  public OrderNotFoundException(UUID orderId) {
    super("no order has the identifier " + orderId);
    this.orderId = orderId;
  }

  /**
   * Returns the identifier that was not found.
   *
   * @return the identifier
   */
  public UUID orderId() {
    return orderId;
  }
}
