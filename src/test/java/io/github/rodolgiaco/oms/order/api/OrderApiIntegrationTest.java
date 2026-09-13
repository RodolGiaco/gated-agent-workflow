package io.github.rodolgiaco.oms.order.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.rodolgiaco.oms.TestcontainersConfiguration;
import io.github.rodolgiaco.oms.order.application.port.out.OrderRepository;
import io.github.rodolgiaco.oms.order.domain.Order;
import io.github.rodolgiaco.oms.order.domain.OrderItem;
import io.github.rodolgiaco.oms.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Goes through the real service and the PostgreSQL adapter. MockMvc is built
// from the context instead of through @AutoConfigureMockMvc, so this class
// shares the cached context, and its container, with the other full-context
// tests.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderApiIntegrationTest {

  @Autowired private WebApplicationContext context;

  @Autowired private OrderRepository repository;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void aCreatedOrderIsPersistedAndCanBeRetrieved() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"items": [
                          {"productId": "BOOK", "quantity": 2, "unitPrice": 12.50},
                          {"productId": "CHIP", "quantity": 1000, "unitPrice": 0.005}
                        ]}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.totalAmount").value(30.0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID orderId = UUID.fromString(JsonPath.read(created, "$.orderId"));

    Order stored = repository.findById(orderId).orElseThrow();
    assertEquals(OrderStatus.CREATED, stored.status());
    assertEquals(
        List.of(
            new OrderItem("BOOK", 2, new BigDecimal("12.50")),
            new OrderItem("CHIP", 1000, new BigDecimal("0.005"))),
        stored.items());

    mockMvc
        .perform(get("/api/orders/{orderId}", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[1].productId").value("CHIP"))
        .andExpect(jsonPath("$.items[1].quantity").value(1000))
        .andExpect(jsonPath("$.totalAmount").value(30.0));
  }

  @Test
  void retrievingAnUnknownOrderReturns404() throws Exception {
    mockMvc
        .perform(get("/api/orders/{orderId}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void anInvalidQuantityReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"items": [{"productId": "BOOK", "quantity": 0, "unitPrice": 12.50}]}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
