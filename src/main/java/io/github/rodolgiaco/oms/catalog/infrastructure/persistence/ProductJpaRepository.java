package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to {@link ProductEntity}, used only by {@link JpaProductRepositoryAdapter}.
 */
interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

  Optional<ProductEntity> findBySku(String sku);

  boolean existsBySku(String sku);
}
