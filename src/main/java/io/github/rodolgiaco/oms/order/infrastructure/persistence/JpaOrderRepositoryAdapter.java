package io.github.rodolgiaco.oms.order.infrastructure.persistence;

import io.github.rodolgiaco.oms.order.application.port.out.OrderRepository;
import io.github.rodolgiaco.oms.order.domain.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Stores {@link Order} aggregates in the relational database through JPA. */
@Component
class JpaOrderRepositoryAdapter implements OrderRepository {

  private final OrderJpaRepository orders;

  JpaOrderRepositoryAdapter(OrderJpaRepository orders) {
    this.orders = orders;
  }

  @Override
  @Transactional
  public void save(Order order) {
    orders.save(OrderEntityMapper.toEntity(order));
  }

  // The items are loaded lazily, so the mapping has to happen inside the
  // transaction that read the order.
  @Override
  @Transactional(readOnly = true)
  public Optional<Order> findById(UUID id) {
    return orders.findById(id).map(OrderEntityMapper::toDomain);
  }
}
