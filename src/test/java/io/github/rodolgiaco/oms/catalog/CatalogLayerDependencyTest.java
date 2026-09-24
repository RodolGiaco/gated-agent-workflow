package io.github.rodolgiaco.oms.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.rodolgiaco.oms.catalog.api.ProductController;
import io.github.rodolgiaco.oms.catalog.application.ProductService;
import io.github.rodolgiaco.oms.catalog.application.port.in.CreateProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.FindProductBySkuUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.GetProductUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.in.ListProductsUseCase;
import io.github.rodolgiaco.oms.catalog.application.port.out.ProductRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Guards which collaborators the catalog API and application layers are allowed to hold. */
class CatalogLayerDependencyTest {

  private static Set<Class<?>> collaboratorsOf(Class<?> type) {
    return Arrays.stream(type.getDeclaredFields())
        .filter(field -> !Modifier.isStatic(field.getModifiers()))
        .map(Field::getType)
        .collect(Collectors.toSet());
  }

  private static final Set<Class<?>> USE_CASES =
      Set.of(
          CreateProductUseCase.class,
          GetProductUseCase.class,
          FindProductBySkuUseCase.class,
          ListProductsUseCase.class);

  @Test
  void theControllerHoldsOnlyTheUseCases() {
    assertEquals(USE_CASES, collaboratorsOf(ProductController.class));
  }

  @Test
  void theControllerIsBuiltOnlyFromTheUseCases() {
    assertEquals(
        USE_CASES,
        Arrays.stream(ProductController.class.getConstructors())
            .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
            .collect(Collectors.toSet()));
  }

  @Test
  void theServiceHoldsOnlyTheRepositoryPort() {
    assertEquals(Set.of(ProductRepository.class), collaboratorsOf(ProductService.class));
  }
}
