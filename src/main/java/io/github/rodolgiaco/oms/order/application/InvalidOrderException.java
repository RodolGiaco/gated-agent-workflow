package io.github.rodolgiaco.oms.order.application;

/** Thrown when the requested items of a new order break an invariant of the order domain. */
public class InvalidOrderException extends RuntimeException {

  /**
   * Creates the exception from the domain's refusal.
   *
   * @param cause the exception the domain threw
   */
  public InvalidOrderException(IllegalArgumentException cause) {
    super(cause.getMessage(), cause);
  }
}
