package io.github.rodolgiaco.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the order management system, the reference application that the issue cycle builds one
 * issue at a time.
 *
 * <p>{@link SpringBootApplication} turns on auto-configuration and makes this package the root of
 * component, entity and repository scanning, so every module under {@code io.github.rodolgiaco.oms}
 * is found without further configuration.
 */
@SpringBootApplication
public class OmsApplication {

  public static void main(String[] args) {
    SpringApplication.run(OmsApplication.class, args);
  }
}
