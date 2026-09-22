package io.github.rodolgiaco.oms.catalog.api;

import java.util.List;

/**
 * Body returned by {@code GET /api/products}.
 *
 * @param products the products on this page, ordered by SKU
 * @param page the zero-based index of this page
 * @param size the most products a page holds
 * @param totalElements how many products the whole catalog holds
 * @param totalPages how many pages of this size the whole catalog fills
 */
public record ProductPageResponse(
    List<ProductResponse> products, int page, int size, long totalElements, int totalPages) {}
