-- Amounts are NUMERIC without a precision or scale on purpose: the domain
-- accepts any positive BigDecimal, and a fixed scale would round a price such
-- as 0.005 to 0.00, which the domain would then refuse to load back.
-- The product identifier is TEXT for the same reason: the domain sets no
-- length limit on it.

CREATE TABLE orders (
    id     UUID        NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE order_items (
    order_id    UUID    NOT NULL,
    line_number INTEGER NOT NULL,
    product_id  TEXT    NOT NULL,
    quantity    INTEGER NOT NULL,
    unit_price  NUMERIC NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (order_id, line_number),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_items_line_number CHECK (line_number >= 0),
    CONSTRAINT ck_order_items_product_id CHECK (btrim(product_id) <> ''),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_unit_price CHECK (unit_price > 0)
);
