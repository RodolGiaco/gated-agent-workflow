package io.github.rodolgiaco.oms.order.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Row of the {@code orders} table, with its rows of {@code order_items}.
 *
 * <p>This is the persistence shape of an order, not the domain aggregate: {@link OrderEntityMapper}
 * converts between the two so that the domain carries no JPA concern.
 */
@Entity
@Table(name = "orders")
class OrderEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  // The status is kept as its name rather than the domain enum, so that this
  // class does not depend on the domain; the mapper converts it.
  @Column(name = "status", nullable = false, length = 32)
  private String status;

  // Items have no identity of their own in the domain, so they are values of
  // the order. The line number keeps them in the order they were given.
  @ElementCollection
  @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
  @OrderColumn(name = "line_number")
  private List<OrderItemEmbeddable> items = new ArrayList<>();

  /** Required by JPA. */
  protected OrderEntity() {}

  OrderEntity(UUID id, String status, List<OrderItemEmbeddable> items) {
    this.id = id;
    this.status = status;
    this.items = new ArrayList<>(items);
  }

  UUID getId() {
    return id;
  }

  String getStatus() {
    return status;
  }

  List<OrderItemEmbeddable> getItems() {
    return items;
  }
}
