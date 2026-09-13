package io.github.rodolgiaco.oms.order.api;

import io.github.rodolgiaco.oms.order.application.port.in.CreateOrderCommand;
import io.github.rodolgiaco.oms.order.domain.Order;

/** Converts between the HTTP shapes of the order API and the application and domain objects. */
final class OrderApiMapper {

  private OrderApiMapper() {}

  // Only called with a request that passed validation, so neither the list,
  // its elements nor the quantities are null.
  static CreateOrderCommand toCommand(CreateOrderRequest request) {
    return new CreateOrderCommand(
        request.items().stream()
            .map(
                item ->
                    new CreateOrderCommand.Item(
                        item.productId(), item.quantity(), item.unitPrice()))
            .toList());
  }

  static OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.id(),
        order.status().name(),
        order.items().stream()
            .map(
                item -> new OrderResponse.Item(item.productId(), item.quantity(), item.unitPrice()))
            .toList(),
        order.total());
  }
}
