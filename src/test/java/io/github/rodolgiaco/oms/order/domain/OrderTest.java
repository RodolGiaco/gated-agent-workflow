package io.github.rodolgiaco.oms.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

  private static final OrderItem BOOK = new OrderItem("BOOK", 2, new BigDecimal("12.50"));

  private static final OrderItem PEN = new OrderItem("PEN", 3, new BigDecimal("1.20"));

  @Test
  void createsAnOrderWithOneItem() {
    Order order = Order.create(List.of(BOOK));

    assertNotNull(order.id());
    assertEquals(List.of(BOOK), order.items());
  }

  @Test
  void createsAnOrderWithSeveralItems() {
    Order order = Order.create(List.of(BOOK, PEN));

    assertNotNull(order.id());
    assertEquals(List.of(BOOK, PEN), order.items());
  }

  @Test
  void aNewOrderHasStatusCreated() {
    assertEquals(OrderStatus.CREATED, Order.create(List.of(BOOK)).status());
  }

  @Test
  void everyNewOrderGetsItsOwnIdentifier() {
    assertNotEquals(Order.create(List.of(BOOK)).id(), Order.create(List.of(BOOK)).id());
  }

  @Test
  void rejectsAnOrderWithoutItems() {
    assertThrows(IllegalArgumentException.class, () -> Order.create(List.of()));
  }

  @Test
  void rejectsANullItemList() {
    assertThrows(IllegalArgumentException.class, () -> Order.create(null));
  }

  @Test
  void rejectsANullItem() {
    assertThrows(IllegalArgumentException.class, () -> Order.create(Arrays.asList(BOOK, null)));
  }

  @Test
  void totalIsTheSumOfQuantityTimesUnitPriceOverEveryItem() {
    // 2 * 12.50 + 3 * 1.20 = 25.00 + 3.60
    assertEquals(new BigDecimal("28.60"), Order.create(List.of(BOOK, PEN)).total());
    assertEquals(new BigDecimal("25.00"), Order.create(List.of(BOOK)).total());
  }

  @Test
  void changingTheListTheOrderWasCreatedFromDoesNotChangeTheOrder() {
    List<OrderItem> items = new ArrayList<>(List.of(BOOK));
    Order order = Order.create(items);

    items.add(PEN);

    assertEquals(List.of(BOOK), order.items());
    assertEquals(new BigDecimal("25.00"), order.total());
  }

  @Test
  void theItemsOfAnOrderCannotBeModified() {
    Order order = Order.create(List.of(BOOK));

    assertThrows(UnsupportedOperationException.class, () -> order.items().add(PEN));
  }
}
