package io.github.rodolgiaco.oms.order.infrastructure.persistence;

import io.github.rodolgiaco.oms.order.domain.Order;
import io.github.rodolgiaco.oms.order.domain.OrderItem;
import io.github.rodolgiaco.oms.order.domain.OrderStatus;
import java.util.List;

/** Converts between the {@link Order} aggregate and its persistence shape, {@link OrderEntity}. */
final class OrderEntityMapper {

  private OrderEntityMapper() {}

  static OrderEntity toEntity(Order order) {
    List<OrderItemEmbeddable> items =
        order.items().stream()
            .map(
                item ->
                    new OrderItemEmbeddable(item.productId(), item.quantity(), item.unitPrice()))
            .toList();
    return new OrderEntity(order.id(), order.status().name(), items);
  }

  /**
   * Rebuilds the aggregate from what was stored.
   *
   * @throws IllegalArgumentException if the stored rows break an invariant of the domain, such as
   *     an unknown status or an order without items
   */
  static Order toDomain(OrderEntity entity) {
    // A gap in the line numbers comes back as a null element, which the
    // aggregate refuses, so it is not checked separately here.
    List<OrderItem> items =
        entity.getItems().stream()
            .map(
                item ->
                    item == null
                        ? null
                        : new OrderItem(
                            item.getProductId(), item.getQuantity(), item.getUnitPrice()))
            .toList();
    return Order.reconstitute(entity.getId(), items, OrderStatus.valueOf(entity.getStatus()));
  }
}
