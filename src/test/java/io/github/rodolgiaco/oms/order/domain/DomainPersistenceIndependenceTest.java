package io.github.rodolgiaco.oms.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Guards that persistence stays out of the domain model. */
class DomainPersistenceIndependenceTest {

  @ParameterizedTest
  @ValueSource(classes = {Order.class, OrderItem.class, OrderStatus.class})
  void theDomainClassCarriesNoPersistenceAnnotation(Class<?> type) {
    List<AnnotatedElement> elements = new ArrayList<>();
    elements.add(type);
    elements.addAll(Arrays.asList(type.getDeclaredFields()));
    elements.addAll(Arrays.asList(type.getDeclaredMethods()));
    elements.addAll(Arrays.asList(type.getDeclaredConstructors()));
    if (type.isRecord()) {
      elements.addAll(Arrays.asList(type.getRecordComponents()));
    }
    Stream.concat(
            Arrays.stream(type.getDeclaredMethods()), Arrays.stream(type.getDeclaredConstructors()))
        .forEach(executable -> elements.addAll(Arrays.asList(executable.getParameters())));

    List<String> found =
        elements.stream()
            .flatMap(element -> Arrays.stream(element.getAnnotations()))
            .map(Annotation::annotationType)
            .map(Class::getName)
            .filter(
                name ->
                    name.startsWith("jakarta.persistence.") || name.startsWith("org.hibernate."))
            .toList();

    assertEquals(List.of(), found);
  }
}
