package io.github.rodolgiaco.oms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HealthTest {

  @Test
  void statusIsOk() {
    assertEquals("ok", Health.status());
  }
}
