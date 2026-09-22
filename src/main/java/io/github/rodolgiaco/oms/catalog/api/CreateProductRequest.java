package io.github.rodolgiaco.oms.catalog.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Body of {@code POST /api/products}.
 *
 * <p>The constraints mirror the invariants of the catalog domain, so an invalid request is refused
 * before it reaches the use case.
 *
 * @param sku the stock keeping unit of the product, never blank
 * @param name the name of the product, never blank
 * @param unitPrice the price of one unit, greater than zero
 */
public record CreateProductRequest(
    @NotBlank String sku, @NotBlank String name, @NotNull @Positive BigDecimal unitPrice) {}
