package io.github.rodolgiaco.gaw;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HealthTest {

  @Test
  void statusIsOk() {
    assertEquals("ok", Health.status());
  }
}
