package io.github.rodolgiaco.oms.catalog.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.rodolgiaco.oms.TestcontainersConfiguration;
import io.github.rodolgiaco.oms.catalog.application.port.out.ProductRepository;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Goes through the real service and the PostgreSQL adapter. MockMvc is built
// from the context instead of through @AutoConfigureMockMvc, so this class
// shares the cached context, and its container, with the other full-context
// tests. The table is emptied before each test so that the pages hold only
// what the test created.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductApiIntegrationTest {

  @Autowired private WebApplicationContext context;

  @Autowired private ProductRepository repository;

  @Autowired private JdbcTemplate jdbc;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    jdbc.update("DELETE FROM catalog.products");
  }

  private ResultActions create(String sku, String name, String unitPrice) throws Exception {
    return mockMvc.perform(
        post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"sku": "%s", "name": "%s", "unitPrice": %s}
                """
                    .formatted(sku, name, unitPrice)));
  }

  @Test
  void aCreatedProductIsPersistedAndCanBeRetrieved() throws Exception {
    String created =
        create("CHIP-1", "Chip", "0.005")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("CHIP-1"))
            .andExpect(jsonPath("$.name").value("Chip"))
            .andExpect(jsonPath("$.unitPrice").value(0.005))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID productId = UUID.fromString(JsonPath.read(created, "$.productId"));

    Product stored = repository.findById(productId).orElseThrow();
    assertEquals("CHIP-1", stored.sku());
    assertEquals("Chip", stored.name());
    assertEquals(new BigDecimal("0.005"), stored.unitPrice());

    mockMvc
        .perform(get("/api/products/{productId}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(productId.toString()))
        .andExpect(jsonPath("$.sku").value("CHIP-1"))
        .andExpect(jsonPath("$.name").value("Chip"))
        .andExpect(jsonPath("$.unitPrice").value(0.005));
  }

  @Test
  void retrievingAnUnknownProductReturns404() throws Exception {
    mockMvc
        .perform(get("/api/products/{productId}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void creatingAProductWithAnExistingSkuReturns409AndStoresNothingMore() throws Exception {
    create("BOOK-1", "Clean Code", "12.50").andExpect(status().isCreated());

    create("BOOK-1", "Another book", "9.99")
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Duplicate SKU"));

    assertEquals(
        List.of("Clean Code"),
        jdbc.queryForList("SELECT name FROM catalog.products WHERE sku = 'BOOK-1'", String.class));
  }

  @Test
  void listingHonoursThePageAndSizeParameters() throws Exception {
    for (String sku : List.of("C", "A", "E", "B", "D")) {
      create(sku, "Product " + sku, "1.00").andExpect(status().isCreated());
    }

    mockMvc
        .perform(get("/api/products").param("page", "1").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products.length()").value(2))
        .andExpect(jsonPath("$.products[0].sku").value("C"))
        .andExpect(jsonPath("$.products[1].sku").value("D"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.totalPages").value(3));

    mockMvc
        .perform(get("/api/products").param("page", "2").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products.length()").value(1))
        .andExpect(jsonPath("$.products[0].sku").value("E"));

    mockMvc
        .perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products.length()").value(5))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20));
  }

  @Test
  void anInvalidProductReturns400AndStoresNothing() throws Exception {
    create("BOOK-1", " ", "12.50")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    create("BOOK-1", "Clean Code", "0")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM catalog.products", Integer.class));
  }
}
