package io.github.rodolgiaco.oms.catalog.application.port.in;

import io.github.rodolgiaco.oms.catalog.application.ProductPage;

/** Input port for browsing the catalog one page at a time. */
public interface ListProductsUseCase {

  /** The most products a caller may ask for in one page. */
  int MAX_PAGE_SIZE = 100;

  /**
   * Returns one page of the catalog, with the products ordered by SKU.
   *
   * @param page the zero-based index of the page, zero or more
   * @param size the most products the page holds, from one to {@link #MAX_PAGE_SIZE}
   * @return the page, with no products when it lies past the end of the catalog
   * @throws IllegalArgumentException if the page is negative or the size is out of range
   */
  ProductPage listProducts(int page, int size);
}
