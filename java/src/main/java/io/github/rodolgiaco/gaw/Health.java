package io.github.rodolgiaco.gaw;

/**
 * Placeholder unit so the build, the formatter and the test runner have something real to act on.
 */
public final class Health {

  private Health() {}

  /**
   * Returns the service status.
   *
   * @return the literal string {@code ok}
   */
  public static String status() {
    return "ok";
  }
}
