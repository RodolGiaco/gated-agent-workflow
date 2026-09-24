package io.github.rodolgiaco.oms.catalog.api;

import io.github.rodolgiaco.oms.catalog.application.DuplicateSkuException;
import io.github.rodolgiaco.oms.catalog.application.InvalidProductException;
import io.github.rodolgiaco.oms.catalog.application.ProductNotFoundException;
import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.FindProductBySkuUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.GetProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.ListProductsUseCase;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Exposes the catalog use cases over HTTP.
 *
 * <p>A {@link RestController} because every method answers with a body written as JSON. It is a
 * singleton that keeps no state besides the use cases, which Spring injects through the
 * constructor. It is the entry point of every catalog request: it maps the HTTP shapes to the use
 * cases and back, and leaves the errors to the global exception handler.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final CreateProductUseCase createProduct;

  private final GetProductUseCase getProduct;

  private final FindProductBySkuUseCase findProductBySku;

  private final ListProductsUseCase listProducts;

  /**
   * Creates the controller on top of the use cases it delegates to.
   *
   * @param createProduct the use case that creates products
   * @param getProduct the use case that retrieves products
   * @param findProductBySku the use case that retrieves products by their SKU
   * @param listProducts the use case that lists products page by page
   */
  public ProductController(
      CreateProductUseCase createProduct,
      GetProductUseCase getProduct,
      FindProductBySkuUseCase findProductBySku,
      ListProductsUseCase listProducts) {
    this.createProduct = createProduct;
    this.getProduct = getProduct;
    this.findProductBySku = findProductBySku;
    this.listProducts = listProducts;
  }

  /**
   * Creates a product.
   *
   * @param request the requested values
   * @return status 201 with the created product and its location
   * @throws InvalidProductException if the domain refuses the values, answered with 400
   * @throws DuplicateSkuException if another product has the SKU, answered with 409
   */
  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
    Product product = createProduct.createProduct(ProductApiMapper.toCommand(request));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{productId}")
            .buildAndExpand(product.id())
            .toUri();
    return ResponseEntity.created(location).body(ProductApiMapper.toResponse(product));
  }

  /**
   * Returns an existing product.
   *
   * @param productId the identifier of the product
   * @return the product
   * @throws ProductNotFoundException if no product has that identifier, answered with 404
   */
  @GetMapping("/{productId}")
  public ProductResponse get(@PathVariable UUID productId) {
    return ProductApiMapper.toResponse(getProduct.getProduct(productId));
  }

  /**
   * Returns the product that has a SKU.
   *
   * @param sku the SKU of the product, compared exactly
   * @return the product
   * @throws ProductNotFoundException if no product has that SKU, answered with 404
   */
  @GetMapping("/sku/{sku}")
  public ProductResponse findBySku(@PathVariable String sku) {
    return ProductApiMapper.toResponse(findProductBySku.findProductBySku(sku));
  }

  /**
   * Returns one page of the catalog, ordered by SKU.
   *
   * <p>The constraints on the parameters make Spring MVC validate them before this method runs, and
   * answer 400 when one is out of range.
   *
   * @param page the zero-based index of the page
   * @param size the most products the page holds
   * @return the page
   */
  @GetMapping
  public ProductPageResponse list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(ListProductsUseCase.MAX_PAGE_SIZE) int size) {
    return ProductApiMapper.toResponse(listProducts.listProducts(page, size));
  }
}
