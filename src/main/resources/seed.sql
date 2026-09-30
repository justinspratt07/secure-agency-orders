INSERT INTO agencies (agency_id, name) VALUES ('AGENCY-A', 'Cedar School District');
INSERT INTO agencies (agency_id, name) VALUES ('AGENCY-B', 'Maple Public Library');
INSERT INTO orders (order_id, agency_id, description, total_amount, status)
VALUES ('ORDER-A100', 'AGENCY-A', 'Classroom art supplies', 125.50, 'OPEN');
INSERT INTO orders (order_id, agency_id, description, total_amount, status)
VALUES ('ORDER-B200', 'AGENCY-B', 'Community workshop supplies', 80.00, 'OPEN');
INSERT INTO products (product_id, name, unit_price) VALUES ('PROD-PAINT', 'Classroom paint set', 25.10);
INSERT INTO products (product_id, name, unit_price) VALUES ('PROD-PAPER', 'Workshop paper pack', 20.00);
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES ('ORDER-A100', 'PROD-PAINT', 5, 25.10);
INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES ('ORDER-B200', 'PROD-PAPER', 4, 20.00);
