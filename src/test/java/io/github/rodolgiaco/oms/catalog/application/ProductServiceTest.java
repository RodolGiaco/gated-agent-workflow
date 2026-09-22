package io.github.rodolgiaco.oms.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductCommand;
import io.github.rodolgiaco.oms.catalog.application.port.in.ListProductsUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.out.ProductRepository;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductServiceTest {

  private static final CreateProductCommand BOOK =
      new CreateProductCommand("BOOK-1", "Clean Code", new BigDecimal("12.50"));

  /** Keeps products in a map, so the service is tested without a database. */
  private static final class InMemoryProductRepository implements ProductRepository {

    private final Map<UUID, Product> products = new HashMap<>();

    @Override
    public void save(Product product) {
      products.put(product.id(), product);
    }

    @Override
    public Optional<Product> findById(UUID id) {
      return Optional.ofNullable(products.get(id));
    }

    @Override
    public boolean existsBySku(String sku) {
      return products.values().stream().anyMatch(product -> product.sku().equals(sku));
    }

    @Override
    public ProductPage findAll(int page, int size) {
      List<Product> content =
          products.values().stream()
              .sorted(Comparator.comparing(Product::sku))
              .skip((long) page * size)
              .limit(size)
              .toList();
      return new ProductPage(content, page, size, products.size());
    }
  }

  private final InMemoryProductRepository repository = new InMemoryProductRepository();

  private final ProductService service = new ProductService(repository);

  @Test
  void createsAProductWithTheRequestedValues() {
    Product product = service.createProduct(BOOK);

    assertNotNull(product.id());
    assertEquals("BOOK-1", product.sku());
    assertEquals("Clean Code", product.name());
    assertEquals(new BigDecimal("12.50"), product.unitPrice());
  }

  @Test
  void theCreatedProductIsStoredThroughTheRepository() {
    Product product = service.createProduct(BOOK);

    assertSame(product, repository.findById(product.id()).orElseThrow());
  }

  @Test
  void refusesASkuThatAnotherProductHasAndStoresNothingMore() {
    Product first = service.createProduct(BOOK);

    DuplicateSkuException thrown =
        assertThrows(
            DuplicateSkuException.class,
            () ->
                service.createProduct(
                    new CreateProductCommand("BOOK-1", "Another book", BigDecimal.ONE)));

    assertEquals("BOOK-1", thrown.sku());
    assertEquals(Map.of(first.id(), first), repository.products);
  }

  @Test
  void refusesABlankSkuAndStoresNothing() {
    assertThrows(
        InvalidProductException.class,
        () -> service.createProduct(new CreateProductCommand(" ", "Clean Code", BigDecimal.ONE)));
    assertTrue(repository.products.isEmpty());
  }

  @Test
  void refusesABlankNameAndStoresNothing() {
    assertThrows(
        InvalidProductException.class,
        () -> service.createProduct(new CreateProductCommand("BOOK-1", "", BigDecimal.ONE)));
    assertTrue(repository.products.isEmpty());
  }

  @Test
  void refusesAnInvalidUnitPriceAndStoresNothing() {
    assertThrows(
        InvalidProductException.class,
        () ->
            service.createProduct(
                new CreateProductCommand("BOOK-1", "Clean Code", BigDecimal.ZERO)));
    assertThrows(
        InvalidProductException.class,
        () ->
            service.createProduct(
                new CreateProductCommand("BOOK-1", "Clean Code", new BigDecimal("-1"))));
    assertThrows(
        InvalidProductException.class,
        () -> service.createProduct(new CreateProductCommand("BOOK-1", "Clean Code", null)));
    assertTrue(repository.products.isEmpty());
  }

  @Test
  void retrievesAStoredProduct() {
    Product product = Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));
    repository.save(product);

    assertSame(product, service.getProduct(product.id()));
  }

  @Test
  void retrievingAnUnknownProductThrowsProductNotFound() {
    UUID unknown = UUID.randomUUID();

    ProductNotFoundException thrown =
        assertThrows(ProductNotFoundException.class, () -> service.getProduct(unknown));

    assertEquals(unknown, thrown.productId());
  }

  @Test
  void listsTheRequestedPageOfProductsOrderedBySku() {
    for (String sku : List.of("C", "A", "E", "B", "D")) {
      service.createProduct(new CreateProductCommand(sku, "Product " + sku, BigDecimal.ONE));
    }

    ProductPage page = service.listProducts(1, 2);

    assertEquals(List.of("C", "D"), page.products().stream().map(Product::sku).toList());
    assertEquals(1, page.page());
    assertEquals(2, page.size());
    assertEquals(5, page.totalElements());
    assertEquals(3, page.totalPages());
  }

  @Test
  void acceptsTheLargestPageSize() {
    assertEquals(
        ListProductsUseCase.MAX_PAGE_SIZE,
        service.listProducts(0, ListProductsUseCase.MAX_PAGE_SIZE).size());
  }

  @ParameterizedTest
  @CsvSource({"-1, 20", "0, 0", "0, -1", "0, 101"})
  void refusesAPageOrSizeOutOfRange(int page, int size) {
    assertThrows(IllegalArgumentException.class, () -> service.listProducts(page, size));
  }
}
