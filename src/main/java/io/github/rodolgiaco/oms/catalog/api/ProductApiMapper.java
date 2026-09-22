package io.github.rodolgiaco.oms.catalog.api;

import io.github.rodolgiaco.oms.catalog.application.ProductPage;
import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductCommand;
import io.github.rodolgiaco.oms.catalog.domain.Product;

/** Converts between the HTTP shapes of the catalog API and the application and domain objects. */
final class ProductApiMapper {

  private ProductApiMapper() {}

  static CreateProductCommand toCommand(CreateProductRequest request) {
    return new CreateProductCommand(request.sku(), request.name(), request.unitPrice());
  }

  static ProductResponse toResponse(Product product) {
    return new ProductResponse(product.id(), product.sku(), product.name(), product.unitPrice());
  }

  static ProductPageResponse toResponse(ProductPage page) {
    return new ProductPageResponse(
        page.products().stream().map(ProductApiMapper::toResponse).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }
}
