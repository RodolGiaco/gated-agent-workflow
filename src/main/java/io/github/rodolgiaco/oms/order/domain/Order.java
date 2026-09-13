package io.github.rodolgiaco.oms.order.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An order placed in the order management system, and the aggregate that guards its invariants.
 *
 * <p>An order always has an identifier, a status, and at least one {@link OrderItem}. Its items
 * cannot change once it is created.
 */
public final class Order {

  private final UUID id;

  private final List<OrderItem> items;

  private final OrderStatus status;

  private Order(UUID id, List<OrderItem> items, OrderStatus status) {
    this.id = id;
    this.items = items;
    this.status = status;
  }

  /**
   * Creates a new order with a fresh identifier and status {@link OrderStatus#CREATED}.
   *
   * @param items the items of the order; the list is copied, so later changes to it do not reach
   *     the order
   * @return the new order
   * @throws IllegalArgumentException if the items are null, empty, or contain a null element
   */
  public static Order create(List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("an order must contain at least one item");
    }
    // Not items.contains(null): the unmodifiable lists of List.of throw on a
    // null argument instead of answering false.
    if (items.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("an order must not contain a null item");
    }
    return new Order(UUID.randomUUID(), List.copyOf(items), OrderStatus.CREATED);
  }

  /**
   * Returns the identifier of this order.
   *
   * @return the identifier, never null
   */
  public UUID id() {
    return id;
  }

  /**
   * Returns the items of this order.
   *
   * @return an unmodifiable list with at least one item
   */
  public List<OrderItem> items() {
    return items;
  }

  /**
   * Returns the status of this order.
   *
   * @return the status, never null
   */
  public OrderStatus status() {
    return status;
  }

  /**
   * Returns what this order costs in total.
   *
   * @return the sum of {@code quantity * unitPrice} over every item
   */
  public BigDecimal total() {
    return items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
