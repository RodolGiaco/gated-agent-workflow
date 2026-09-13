package io.github.rodolgiaco.oms.order.application.port.in;

import io.github.rodolgiaco.oms.order.application.InvalidOrderException;
import io.github.rodolgiaco.oms.order.domain.Order;

/** Input port for creating a new {@link Order}. */
public interface CreateOrderUseCase {

  /**
   * Creates an order from the requested items and stores it.
   *
   * @param command the requested items, never null
   * @return the order as it was stored
   * @throws InvalidOrderException if the requested items do not make a valid order
   */
  Order createOrder(CreateOrderCommand command);
}
