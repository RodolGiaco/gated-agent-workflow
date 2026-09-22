-- Flyway runs this with the catalog schema as its default schema, so the
-- table is created there and not next to the orders tables.
-- The unit price is NUMERIC without a precision or scale, and the SKU and the
-- name are TEXT, for the same reasons as in the orders schema: the domain
-- accepts any positive BigDecimal and sets no length limit on either text.
-- The SKU is unique so that two products created at the same time cannot
-- both take it.

CREATE TABLE products (
    id         UUID    NOT NULL,
    sku        TEXT    NOT NULL,
    name       TEXT    NOT NULL,
    unit_price NUMERIC NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_sku CHECK (btrim(sku) <> ''),
    CONSTRAINT ck_products_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_products_unit_price CHECK (unit_price > 0)
);
