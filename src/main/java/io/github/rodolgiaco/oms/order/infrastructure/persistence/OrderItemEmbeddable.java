package io.github.rodolgiaco.oms.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Row of the {@code order_items} table, owned by an {@link OrderEntity}. */
@Embeddable
class OrderItemEmbeddable {

  @Column(name = "product_id", nullable = false)
  private String productId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  /** Required by JPA. */
  protected OrderItemEmbeddable() {}

  OrderItemEmbeddable(String productId, int quantity, BigDecimal unitPrice) {
    this.productId = productId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
  }

  String getProductId() {
    return productId;
  }

  int getQuantity() {
    return quantity;
  }

  BigDecimal getUnitPrice() {
    return unitPrice;
  }
}
