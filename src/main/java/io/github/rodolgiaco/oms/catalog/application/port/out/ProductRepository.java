package io.github.rodolgiaco.oms.catalog.application.port.out;

import io.github.rodolgiaco.oms.catalog.application.DuplicateSkuException;
import io.github.rodolgiaco.oms.catalog.application.ProductPage;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.util.Optional;
import java.util.UUID;

/** Output port through which the application stores and loads {@link Product} models. */
public interface ProductRepository {

  /**
   * Stores a product, replacing any product stored under the same identifier.
   *
   * @param product the product to store, never null
   * @throws DuplicateSkuException if a product with another identifier already has the same SKU,
   *     such as one stored concurrently after the caller checked {@link #existsBySku(String)}
   */
  void save(Product product);

  /**
   * Loads the product with the given identifier.
   *
   * @param id the identifier of the product, never null
   * @return the product, or empty when no product has that identifier
   */
  Optional<Product> findById(UUID id);

  /**
   * Tells whether a product with the given SKU is stored.
   *
   * @param sku the SKU to look for, compared exactly, never null
   * @return whether a product has that SKU
   */
  boolean existsBySku(String sku);

  /**
   * Loads one page of the stored products, ordered by SKU.
   *
   * @param page the zero-based index of the page, zero or more
   * @param size the most products the page holds, at least one
   * @return the page, with no products when it lies past the last product
   */
  ProductPage findAll(int page, int size);
}
