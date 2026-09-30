-- Disposable Compose environment only. No database port is published to the host.
CREATE USER 'orders_app'@'%' IDENTIFIED BY 'disposable-demo-only';
GRANT SELECT, INSERT, UPDATE ON agency_orders.orders TO 'orders_app'@'%';
GRANT SELECT, INSERT ON agency_orders.order_items TO 'orders_app'@'%';
GRANT SELECT ON agency_orders.products TO 'orders_app'@'%';
GRANT SELECT ON agency_orders.agencies TO 'orders_app'@'%';
GRANT INSERT ON agency_orders.audit_events TO 'orders_app'@'%';
