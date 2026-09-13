package io.github.rodolgiaco.oms.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * Body of {@code POST /api/orders}.
 *
 * <p>The constraints mirror the invariants of the order domain, so an invalid request is refused
 * before it reaches the use case.
 *
 * @param items the lines of the order, at least one
 */
public record CreateOrderRequest(@NotEmpty List<@NotNull @Valid Item> items) {

  /**
   * One line of the requested order.
   *
   * @param productId the identifier of the product, never blank
   * @param quantity the number of units, greater than zero
   * @param unitPrice the price of one unit, greater than zero
   */
  public record Item(
      @NotBlank String productId,
      @NotNull @Positive Integer quantity,
      @NotNull @Positive BigDecimal unitPrice) {}
}
