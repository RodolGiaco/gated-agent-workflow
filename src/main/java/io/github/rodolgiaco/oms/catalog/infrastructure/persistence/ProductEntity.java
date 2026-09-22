package io.github.rodolgiaco.oms.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Row of the {@code catalog.products} table.
 *
 * <p>This is the persistence shape of a product, not the domain model: {@link ProductEntityMapper}
 * converts between the two so that the domain carries no JPA concern.
 */
@Entity
@Table(name = "products", schema = CatalogSchemaConfiguration.SCHEMA)
class ProductEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "sku", nullable = false)
  private String sku;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  /** Required by JPA. */
  protected ProductEntity() {}

  ProductEntity(UUID id, String sku, String name, BigDecimal unitPrice) {
    this.id = id;
    this.sku = sku;
    this.name = name;
    this.unitPrice = unitPrice;
  }

  UUID getId() {
    return id;
  }

  String getSku() {
    return sku;
  }

  String getName() {
    return name;
  }

  BigDecimal getUnitPrice() {
    return unitPrice;
  }
}
