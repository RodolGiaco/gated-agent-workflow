package io.github.rodolgiaco.oms.web;

import io.github.rodolgiaco.oms.order.application.InvalidOrderException;
import io.github.rodolgiaco.oms.order.application.OrderNotFoundException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns the exceptions of every controller into {@link ProblemDetail} responses.
 *
 * <p>The exceptions Spring MVC raises itself, such as an unreadable body or a malformed path
 * variable, are answered by the superclass.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * One constraint a request broke.
   *
   * @param field the path of the offending field, such as {@code items[0].quantity}
   * @param message why the value was refused
   */
  public record FieldViolation(String field, String message) {}

  @ExceptionHandler(OrderNotFoundException.class)
  ProblemDetail handleOrderNotFound(OrderNotFoundException e) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    problem.setTitle("Order not found");
    return problem;
  }

  @ExceptionHandler(InvalidOrderException.class)
  ProblemDetail handleInvalidOrder(InvalidOrderException e) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problem.setTitle("Invalid order");
    return problem;
  }

  // Adds which fields broke which constraint to the problem the superclass
  // builds, which only says that the content is invalid.
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldViolation> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();
    ex.getBody().setProperty("errors", violations);
    return super.handleMethodArgumentNotValid(ex, headers, status, request);
  }
}
