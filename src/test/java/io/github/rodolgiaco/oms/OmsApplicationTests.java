package io.github.rodolgiaco.oms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Starts the whole application against the PostgreSQL the container provides.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OmsApplicationTests {

  @Autowired private WebApplicationContext context;

  // The context fails to start before this body runs if any bean cannot be
  // created, so reaching the request already proves the application starts.
  // MockMvc is built from the context, as in the API integration tests, so
  // this class shares their cached context and container.
  @Test
  void healthReportsUp() throws Exception {
    MockMvcBuilders.webAppContextSetup(context)
        .build()
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
