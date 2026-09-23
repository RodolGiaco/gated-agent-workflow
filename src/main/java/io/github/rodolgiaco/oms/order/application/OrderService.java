package io.github.rodolgiaco.oms.order.application;

import io.github.rodolgiaco.oms.order.application.port.in.CreateOrderCommand;
import io.github.rodolgiaco.oms.order.application.port.in.CreateOrderUseCase;
import io.github.rodolgiaco.oms.order.application.port.in.GetOrderUseCase;
import io.github.rodolgiaco.oms.order.application.port.out.OrderRepository;
import io.github.rodolgiaco.oms.order.domain.Order;
import io.github.rodolgiaco.oms.order.domain.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Carries out the order use cases with the order domain and the {@link OrderRepository} port.
 *
 * <p>A {@link Service} because it holds the application logic between the API and the persistence
 * adapter. It is a singleton, so it keeps no state of its own besides the port, which Spring
 * injects through the constructor. The controller reaches it only through the use case interfaces.
 */
@Service
public class OrderService implements CreateOrderUseCase, GetOrderUseCase {

  private final OrderRepository orders;

  /**
   * Creates the service on top of the given port.
   *
   * @param orders where orders are stored and loaded
   */
  public OrderService(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public Order createOrder(CreateOrderCommand command) {
    Order order;
    try {
      order = Order.create(toItems(command.items()));
    } catch (IllegalArgumentException e) {
      throw new InvalidOrderException(e);
    }
    orders.save(order);
    return order;
  }

  @Override
  public Order getOrder(UUID orderId) {
    return orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
  }

  // A null list or a null line is passed on as such, so the aggregate is the
  // one that refuses it.
  private static List<OrderItem> toItems(List<CreateOrderCommand.Item> items) {
    if (items == null) {
      return null;
    }
    return items.stream()
        .map(
            item ->
                item == null
                    ? null
                    : new OrderItem(item.productId(), item.quantity(), item.unitPrice()))
        .toList();
  }
}
