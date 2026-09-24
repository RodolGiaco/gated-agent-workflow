package io.github.rodolgiaco.oms.catalog.application;

import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductCommand;
import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.FindProductBySkuUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.GetProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.ListProductsUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.out.ProductRepository;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Carries out the catalog use cases with the catalog domain and the {@link ProductRepository} port.
 *
 * <p>A {@link Service} because it holds the application logic between the API and the persistence
 * adapter. It is a singleton, so it keeps no state of its own besides the port, which Spring
 * injects through the constructor. The controller reaches it only through the use case interfaces.
 */
@Service
public class ProductService
    implements CreateProductUseCase,
        GetProductUseCase,
        FindProductBySkuUseCase,
        ListProductsUseCase {

  private final ProductRepository products;

  /**
   * Creates the service on top of the given port.
   *
   * @param products where products are stored and loaded
   */
  public ProductService(ProductRepository products) {
    this.products = products;
  }

  // The SKU is checked before saving so the usual duplicate is refused without
  // a failed write; the port still refuses one stored concurrently in between.
  @Override
  public Product createProduct(CreateProductCommand command) {
    Product product;
    try {
      product = Product.create(command.sku(), command.name(), command.unitPrice());
    } catch (IllegalArgumentException e) {
      throw new InvalidProductException(e);
    }
    if (products.existsBySku(product.sku())) {
      throw new DuplicateSkuException(product.sku());
    }
    products.save(product);
    return product;
  }

  @Override
  public Product getProduct(UUID productId) {
    return products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
  }

  @Override
  public Product findProductBySku(String sku) {
    return products.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
  }

  @Override
  public ProductPage listProducts(int page, int size) {
    if (page < 0) {
      throw new IllegalArgumentException("the page must not be negative, but was " + page);
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException(
          "the page size must be between 1 and " + MAX_PAGE_SIZE + ", but was " + size);
    }
    return products.findAll(page, size);
  }
}
