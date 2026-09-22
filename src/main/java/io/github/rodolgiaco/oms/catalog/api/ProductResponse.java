package io.github.rodolgiaco.oms.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Body returned for a product by {@code POST /api/products} and {@code GET
 * /api/products/{productId}}, and for each product of a page.
 *
 * @param productId the identifier of the product
 * @param sku the stock keeping unit of the product
 * @param name the name of the product
 * @param unitPrice the price of one unit
 */
public record ProductResponse(UUID productId, String sku, String name, BigDecimal unitPrice) {}
