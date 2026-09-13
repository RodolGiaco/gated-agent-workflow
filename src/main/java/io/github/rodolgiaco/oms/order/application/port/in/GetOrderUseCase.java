package io.github.rodolgiaco.oms.order.application.port.in;

import io.github.rodolgiaco.oms.order.application.OrderNotFoundException;
import io.github.rodolgiaco.oms.order.domain.Order;
import java.util.UUID;

/** Input port for retrieving an existing {@link Order}. */
public interface GetOrderUseCase {

  /**
   * Returns the order with the given identifier.
   *
   * @param orderId the identifier of the order, never null
   * @return the order
   * @throws OrderNotFoundException if no order has that identifier
   */
  Order getOrder(UUID orderId);
}
