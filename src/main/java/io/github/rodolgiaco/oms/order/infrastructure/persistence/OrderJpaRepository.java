package io.github.rodolgiaco.oms.order.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@link OrderEntity}, used only by {@link JpaOrderRepositoryAdapter}. */
interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {}
