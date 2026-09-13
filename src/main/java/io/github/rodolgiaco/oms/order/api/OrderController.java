package io.github.rodolgiaco.oms.order.api;

import io.github.rodolgiaco.oms.order.application.port.in.CreateOrderUseCase;
import io.github.rodolgiaco.oms.order.application.port.in.GetOrderUseCase;
import io.github.rodolgiaco.oms.order.domain.Order;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Exposes the order use cases over HTTP. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final CreateOrderUseCase createOrder;

  private final GetOrderUseCase getOrder;

  /**
   * Creates the controller on top of the use cases it delegates to.
   *
   * @param createOrder the use case that creates orders
   * @param getOrder the use case that retrieves orders
   */
  public OrderController(CreateOrderUseCase createOrder, GetOrderUseCase getOrder) {
    this.createOrder = createOrder;
    this.getOrder = getOrder;
  }

  /**
   * Creates an order.
   *
   * @param request the requested items
   * @return status 201 with the created order and its location
   */
  @PostMapping
  public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    Order order = createOrder.createOrder(OrderApiMapper.toCommand(request));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{orderId}")
            .buildAndExpand(order.id())
            .toUri();
    return ResponseEntity.created(location).body(OrderApiMapper.toResponse(order));
  }

  /**
   * Returns an existing order.
   *
   * @param orderId the identifier of the order
   * @return the order
   */
  @GetMapping("/{orderId}")
  public OrderResponse get(@PathVariable UUID orderId) {
    return OrderApiMapper.toResponse(getOrder.getOrder(orderId));
  }
}
