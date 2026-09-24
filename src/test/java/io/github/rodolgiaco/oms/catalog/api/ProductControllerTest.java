package io.github.rodolgiaco.oms.catalog.api;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.rodolgiaco.oms.catalog.application.DuplicateSkuException;
import io.github.rodolgiaco.oms.catalog.application.InvalidProductException;
import io.github.rodolgiaco.oms.catalog.application.ProductNotFoundException;
import io.github.rodolgiaco.oms.catalog.application.ProductPage;
import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductCommand;
import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.FindProductBySkuUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.GetProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.ListProductsUseCase;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  private static final Product BOOK =
      Product.reconstitute(
          UUID.fromString("5a0f3c1e-8d2b-4c6a-9e7f-1b3d5f7a9c21"),
          "BOOK-1",
          "Clean Code",
          new BigDecimal("12.50"));

  private static final Product PEN =
      Product.reconstitute(
          UUID.fromString("0c9e2b4d-6f1a-4e3c-8b5d-7a9f1c3e5b72"),
          "PEN-1",
          "Blue pen",
          new BigDecimal("1.20"));

  private static final String VALID_REQUEST =
      """
      {"sku": "BOOK-1", "name": "Clean Code", "unitPrice": 12.50}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateProductUseCase createProduct;

  @MockitoBean private GetProductUseCase getProduct;

  @MockitoBean private FindProductBySkuUseCase findProductBySku;

  @MockitoBean private ListProductsUseCase listProducts;

  @Test
  void creatingAValidProductReturns201WithTheProduct() throws Exception {
    when(createProduct.createProduct(any())).thenReturn(BOOK);

    mockMvc
        .perform(
            post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/products/" + BOOK.id())))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.productId").value(BOOK.id().toString()))
        .andExpect(jsonPath("$.sku").value("BOOK-1"))
        .andExpect(jsonPath("$.name").value("Clean Code"))
        .andExpect(jsonPath("$.unitPrice").value(12.50));
  }

  @Test
  void theRequestIsMappedToACommandWithTheSameValues() throws Exception {
    when(createProduct.createProduct(any())).thenReturn(BOOK);

    mockMvc
        .perform(
            post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isCreated());

    verify(createProduct)
        .createProduct(new CreateProductCommand("BOOK-1", "Clean Code", new BigDecimal("12.50")));
  }

  @Test
  void retrievingAnExistingProductReturns200WithTheProduct() throws Exception {
    when(getProduct.getProduct(BOOK.id())).thenReturn(BOOK);

    mockMvc
        .perform(get("/api/products/{productId}", BOOK.id()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.productId").value(BOOK.id().toString()))
        .andExpect(jsonPath("$.sku").value("BOOK-1"))
        .andExpect(jsonPath("$.name").value("Clean Code"))
        .andExpect(jsonPath("$.unitPrice").value(12.50));
  }

  @Test
  void retrievingAnUnknownProductReturns404AsAProblemDetail() throws Exception {
    UUID unknown = UUID.randomUUID();
    when(getProduct.getProduct(unknown)).thenThrow(new ProductNotFoundException(unknown));

    mockMvc
        .perform(get("/api/products/{productId}", unknown))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Product not found"))
        .andExpect(jsonPath("$.detail").value("no product has the identifier " + unknown))
        .andExpect(jsonPath("$.instance").value("/api/products/" + unknown));
  }

  @Test
  void retrievingWithAMalformedIdentifierReturns400AsAProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/products/{productId}", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(getProduct);
  }

  @Test
  void findingAProductBySkuReturns200WithTheProduct() throws Exception {
    when(findProductBySku.findProductBySku("BOOK-1")).thenReturn(BOOK);

    mockMvc
        .perform(get("/api/products/sku/{sku}", "BOOK-1"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.productId").value(BOOK.id().toString()))
        .andExpect(jsonPath("$.sku").value("BOOK-1"))
        .andExpect(jsonPath("$.name").value("Clean Code"))
        .andExpect(jsonPath("$.unitPrice").value(12.50));

    verifyNoInteractions(getProduct);
  }

  @Test
  void findingAnUnknownSkuReturns404AsAProblemDetail() throws Exception {
    when(findProductBySku.findProductBySku("NOPE-1"))
        .thenThrow(new ProductNotFoundException("NOPE-1"));

    mockMvc
        .perform(get("/api/products/sku/{sku}", "NOPE-1"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Product not found"))
        .andExpect(jsonPath("$.detail").value("no product has the SKU NOPE-1"))
        .andExpect(jsonPath("$.instance").value("/api/products/sku/NOPE-1"));
  }

  @Test
  void listingReturns200WithThePageOfProducts() throws Exception {
    when(listProducts.listProducts(1, 2)).thenReturn(new ProductPage(List.of(BOOK, PEN), 1, 2, 5));

    mockMvc
        .perform(get("/api/products").param("page", "1").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.products.length()").value(2))
        .andExpect(jsonPath("$.products[0].productId").value(BOOK.id().toString()))
        .andExpect(jsonPath("$.products[0].sku").value("BOOK-1"))
        .andExpect(jsonPath("$.products[0].name").value("Clean Code"))
        .andExpect(jsonPath("$.products[0].unitPrice").value(12.50))
        .andExpect(jsonPath("$.products[1].sku").value("PEN-1"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(3));

    verify(listProducts).listProducts(1, 2);
  }

  @Test
  void listingWithoutParametersAsksForTheFirstPageOfTwenty() throws Exception {
    when(listProducts.listProducts(0, 20)).thenReturn(new ProductPage(List.of(), 0, 20, 0));

    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products.length()").value(0))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.totalPages").value(0));

    verify(listProducts).listProducts(0, 20);
  }

  @ParameterizedTest
  @CsvSource({"-1, 20", "0, 0", "0, -1", "0, 101", "first, 20", "0, all"})
  void listingWithAPageOrSizeOutOfRangeReturns400BeforeReachingTheUseCase(String page, String size)
      throws Exception {
    mockMvc
        .perform(get("/api/products").param("page", page).param("size", size))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(listProducts);
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"\"", "\" \"", "null"})
  void aBlankSkuReturns400BeforeReachingTheUseCase(String sku) throws Exception {
    String request =
        """
        {"sku": %s, "name": "Clean Code", "unitPrice": 12.50}
        """
            .formatted(sku);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors[*].field", hasItem("sku")));

    verifyNoInteractions(createProduct);
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"\"", "\" \"", "null"})
  void aBlankNameReturns400BeforeReachingTheUseCase(String name) throws Exception {
    String request =
        """
        {"sku": "BOOK-1", "name": %s, "unitPrice": 12.50}
        """
            .formatted(name);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors[*].field", hasItem("name")));

    verifyNoInteractions(createProduct);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.00", "-0.01", "-12.50", "null"})
  void aZeroNegativeOrMissingUnitPriceReturns400BeforeReachingTheUseCase(String unitPrice)
      throws Exception {
    String request =
        """
        {"sku": "BOOK-1", "name": "Clean Code", "unitPrice": %s}
        """
            .formatted(unitPrice);

    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors[*].field", hasItem("unitPrice")));

    verifyNoInteractions(createProduct);
  }

  @Test
  void anEmptyRequestReports400WithEveryMissingField() throws Exception {
    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors.length()").value(3));

    verifyNoInteractions(createProduct);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not json",
        "{\"sku\": \"BOOK-1\", \"name\": \"Clean Code\", \"unitPrice\": \"x\"}"
      })
  void anUnreadableBodyReturns400BeforeReachingTheUseCase(String request) throws Exception {
    mockMvc
        .perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));

    verifyNoInteractions(createProduct);
  }

  @Test
  void aProductTheDomainRefusesReturns400AsAProblemDetail() throws Exception {
    when(createProduct.createProduct(any()))
        .thenThrow(new InvalidProductException(new IllegalArgumentException("refused")));

    mockMvc
        .perform(
            post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid product"))
        .andExpect(jsonPath("$.detail").value("refused"));
  }

  @Test
  void creatingAProductWithADuplicateSkuReturns409AsAProblemDetail() throws Exception {
    when(createProduct.createProduct(any())).thenThrow(new DuplicateSkuException("BOOK-1"));

    mockMvc
        .perform(
            post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Duplicate SKU"))
        .andExpect(jsonPath("$.detail").value("a product with the SKU BOOK-1 already exists"))
        .andExpect(jsonPath("$.instance").value("/api/products"));
  }
}
