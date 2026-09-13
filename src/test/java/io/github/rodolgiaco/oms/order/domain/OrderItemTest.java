package io.github.rodolgiaco.oms.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemTest {

  @Test
  void keepsTheValuesItWasCreatedWith() {
    OrderItem item = new OrderItem("SKU-1", 3, new BigDecimal("9.99"));

    assertEquals("SKU-1", item.productId());
    assertEquals(3, item.quantity());
    assertEquals(new BigDecimal("9.99"), item.unitPrice());
  }

  @Test
  void subtotalIsTheQuantityTimesTheUnitPrice() {
    assertEquals(
        new BigDecimal("29.97"), new OrderItem("SKU-1", 3, new BigDecimal("9.99")).subtotal());
    assertEquals(
        new BigDecimal("0.01"), new OrderItem("SKU-2", 1, new BigDecimal("0.01")).subtotal());
  }

  @Test
  void rejectsAZeroQuantity() {
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem("SKU-1", 0, new BigDecimal("9.99")));
  }

  @Test
  void rejectsANegativeQuantity() {
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem("SKU-1", -1, new BigDecimal("9.99")));
  }

  @Test
  void rejectsAZeroUnitPrice() {
    assertThrows(IllegalArgumentException.class, () -> new OrderItem("SKU-1", 1, BigDecimal.ZERO));
    // A zero with a scale is still zero.
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem("SKU-1", 1, new BigDecimal("0.00")));
  }

  @Test
  void rejectsANegativeUnitPrice() {
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem("SKU-1", 1, new BigDecimal("-0.01")));
  }

  @Test
  void rejectsANullUnitPrice() {
    assertThrows(IllegalArgumentException.class, () -> new OrderItem("SKU-1", 1, null));
  }

  @Test
  void rejectsANullOrBlankProductIdentifier() {
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem(null, 1, new BigDecimal("9.99")));
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem("", 1, new BigDecimal("9.99")));
    assertThrows(
        IllegalArgumentException.class, () -> new OrderItem("   ", 1, new BigDecimal("9.99")));
  }
}
