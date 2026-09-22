package io.github.rodolgiaco.oms.catalog.application;

import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.util.List;

/**
 * One page of the catalog, as the application hands it out.
 *
 * <p>The application defines its own page instead of using the one of Spring Data, so that neither
 * the use cases nor their callers depend on the persistence technology.
 *
 * @param products the products on this page, in catalog order; the list is copied
 * @param page the zero-based index of this page
 * @param size the most products a page holds, at least one
 * @param totalElements how many products the whole catalog holds
 */
public record ProductPage(List<Product> products, int page, int size, long totalElements) {

  /**
   * Creates a page and checks that its size can divide the catalog.
   *
   * @throws IllegalArgumentException if the size is zero or negative
   */
  public ProductPage {
    if (size < 1) {
      throw new IllegalArgumentException("the page size must be at least one, but was " + size);
    }
    products = List.copyOf(products);
  }

  /**
   * Returns how many pages of this size the whole catalog fills.
   *
   * @return the number of pages, zero when the catalog is empty
   */
  public int totalPages() {
    return (int) ((totalElements + size - 1) / size);
  }
}
