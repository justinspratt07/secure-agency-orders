CREATE TABLE agencies (
    agency_id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);
CREATE TABLE products (
    product_id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0)
);
CREATE TABLE orders (
    order_id VARCHAR(40) PRIMARY KEY,
    agency_id VARCHAR(40) NOT NULL,
    description VARCHAR(200) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL CHECK (total_amount > 0),
    status VARCHAR(16) NOT NULL CHECK (CASE status WHEN 'OPEN' THEN 1 WHEN 'CANCELLED' THEN 1 ELSE 0 END = 1),
    FOREIGN KEY (agency_id) REFERENCES agencies(agency_id)
);
CREATE INDEX idx_orders_agency ON orders(agency_id);
CREATE TABLE order_items (
    order_id VARCHAR(40) NOT NULL,
    product_id VARCHAR(40) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 1000),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
CREATE TABLE audit_events (
    event_id VARCHAR(40) PRIMARY KEY,
    actor_id VARCHAR(40) NOT NULL,
    agency_id VARCHAR(40) NOT NULL,
    action VARCHAR(24) NOT NULL,
    order_id VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (agency_id) REFERENCES agencies(agency_id)
);
