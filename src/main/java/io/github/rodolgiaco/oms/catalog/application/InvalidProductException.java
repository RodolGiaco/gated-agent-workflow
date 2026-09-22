package io.github.rodolgiaco.oms.catalog.application;

/** Thrown when the requested values of a new product break an invariant of the catalog domain. */
public class InvalidProductException extends RuntimeException {

  /**
   * Creates the exception from the domain's refusal.
   *
   * @param cause the exception the domain threw
   */
  public InvalidProductException(IllegalArgumentException cause) {
    super(cause.getMessage(), cause);
  }
}
