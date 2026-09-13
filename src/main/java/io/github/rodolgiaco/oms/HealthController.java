package io.github.rodolgiaco.oms;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports that the application is up, before any domain module exists to say more. */
@RestController
@RequestMapping("/api")
public class HealthController {

  /** Body of the health response: a single {@code status} field. */
  public record HealthResponse(String status) {}

  private static final HealthResponse UP = new HealthResponse("UP");

  /**
   * Returns the health of the application.
   *
   * @return a body whose {@code status} is {@code UP}
   */
  @GetMapping("/health")
  public HealthResponse health() {
    return UP;
  }
}
