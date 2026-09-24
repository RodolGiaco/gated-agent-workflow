package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import io.github.rodolgiaco.oms.catalog.application.DuplicateSkuException;
import io.github.rodolgiaco.oms.catalog.application.ProductPage;
import io.github.rodolgiaco.oms.catalog.application.port.out.ProductRepository;
import io.github.rodolgiaco.oms.catalog.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores {@link Product} models in the relational database through JPA.
 *
 * <p>A {@link Component} rather than a {@code @Repository}: the Spring Data interface it delegates
 * to already translates persistence exceptions, and this class only adapts it to the port. It is a
 * singleton without state besides that interface, injected through the constructor. The service
 * reaches it only through {@link ProductRepository}.
 */
@Component
class JpaProductRepositoryAdapter implements ProductRepository {

  // Named in V1__create_products.sql.
  static final String SKU_CONSTRAINT = "uk_products_sku";

  private final ProductJpaRepository products;

  JpaProductRepositoryAdapter(ProductJpaRepository products) {
    this.products = products;
  }

  // Flushing makes the insert run here rather than at commit, so a SKU taken
  // concurrently surfaces inside this method, where it can be translated.
  @Override
  @Transactional
  public void save(Product product) {
    try {
      products.saveAndFlush(ProductEntityMapper.toEntity(product));
    } catch (DataIntegrityViolationException e) {
      if (violatesSkuConstraint(e)) {
        throw new DuplicateSkuException(product.sku());
      }
      throw e;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Product> findById(UUID id) {
    return products.findById(id).map(ProductEntityMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Product> findBySku(String sku) {
    return products.findBySku(sku).map(ProductEntityMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsBySku(String sku) {
    return products.existsBySku(sku);
  }

  // The SKU is unique, so ordering by it alone gives every product one place
  // and the pages neither repeat nor skip a product.
  @Override
  @Transactional(readOnly = true)
  public ProductPage findAll(int page, int size) {
    Page<ProductEntity> found = products.findAll(PageRequest.of(page, size, Sort.by("sku")));
    return new ProductPage(
        found.getContent().stream().map(ProductEntityMapper::toDomain).toList(),
        page,
        size,
        found.getTotalElements());
  }

  private static boolean violatesSkuConstraint(DataIntegrityViolationException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        return SKU_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName());
      }
    }
    return false;
  }
}
