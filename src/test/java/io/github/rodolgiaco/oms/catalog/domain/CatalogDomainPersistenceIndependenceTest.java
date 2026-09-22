package io.github.rodolgiaco.oms.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Guards that persistence stays out of the catalog domain model. */
class CatalogDomainPersistenceIndependenceTest {

  @ParameterizedTest
  @ValueSource(classes = {Product.class})
  void theDomainClassCarriesNoPersistenceAnnotation(Class<?> type) {
    List<AnnotatedElement> elements = new ArrayList<>();
    elements.add(type);
    elements.addAll(Arrays.asList(type.getDeclaredFields()));
    elements.addAll(Arrays.asList(type.getDeclaredMethods()));
    elements.addAll(Arrays.asList(type.getDeclaredConstructors()));
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
