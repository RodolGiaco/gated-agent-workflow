package io.github.rodolgiaco.oms.catalog.application.port.in;

import java.math.BigDecimal;

/**
 * What a caller asks for when it creates a product.
 *
 * <p>The values are not checked here: the catalog domain decides whether they make a valid product.
 *
 * @param sku the stock keeping unit of the product
 * @param name the name of the product
 * @param unitPrice the price of one unit
 */
public record CreateProductCommand(String sku, String name, BigDecimal unitPrice) {}
