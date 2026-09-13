package io.github.rodolgiaco.oms;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class GateAgentWorkflowApplicationTests {

  @Autowired private ApplicationContext context;

  // The context fails to start before this body runs if any bean cannot be
  // created, so reaching the assertion already proves the application starts.
  @Test
  void contextLoads() {
    assertNotNull(context.getBean(HealthController.class));
  }
}
