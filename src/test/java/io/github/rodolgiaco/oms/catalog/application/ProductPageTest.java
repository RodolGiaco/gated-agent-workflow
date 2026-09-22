package io.github.rodolgiaco.oms.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductPageTest {

  private static final Product BOOK =
      Product.create("BOOK-1", "Clean Code", new BigDecimal("12.50"));

  @ParameterizedTest
  @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "5, 2, 3", "100, 1, 100"})
  void totalPagesRoundsUpTheCatalogOverThePageSize(long totalElements, int size, int expected) {
    assertEquals(expected, new ProductPage(List.of(), 0, size, totalElements).totalPages());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsASizeBelowOne(int size) {
    assertThrows(IllegalArgumentException.class, () -> new ProductPage(List.of(), 0, size, 0));
  }

  @Test
  void changingTheListThePageWasCreatedFromDoesNotChangeThePage() {
    List<Product> products = new ArrayList<>(List.of(BOOK));
    ProductPage page = new ProductPage(products, 0, 20, 1);

    products.clear();

    assertEquals(List.of(BOOK), page.products());
  }
}
