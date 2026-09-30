package io.github.justinspratt.orders;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Every order read and update is scoped to the trusted actor's agency. */
public final class OrderService {
    private final Database database;
    public OrderService(Database database) { this.database = Objects.requireNonNull(database); }

    public Optional<Order> find(Actor actor, String id) throws SQLException {
        Objects.requireNonNull(actor);
        Actor.validateId(id);
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT order_id, agency_id, description, total_amount, status FROM orders WHERE order_id = ? AND agency_id = ?")) {
            ps.setString(1, id);
            ps.setString(2, actor.agencyId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    public List<Order> list(Actor actor) throws SQLException {
        Objects.requireNonNull(actor);
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(
                "SELECT order_id, agency_id, description, total_amount, status FROM orders WHERE agency_id = ? ORDER BY order_id")) {
            ps.setString(1, actor.agencyId());
            try (ResultSet rs = ps.executeQuery()) {
                List<Order> result = new ArrayList<>();
                while (rs.next()) result.add(read(rs));
                return List.copyOf(result);
            }
        }
    }

    public Order create(Actor actor, String id, String description, List<LineRequest> requested) throws SQLException {
        requireBuyer(actor);
        Actor.validateId(id);
        if (description == null || description.isBlank() || description.length() > 200 ||
                description.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Description must be 1-200 printable characters");
        if (requested == null || requested.isEmpty() || requested.size() > 50)
            throw new IllegalArgumentException("An order requires 1-50 distinct products");
        List<LineRequest> lines = List.copyOf(requested);
        Set<String> ids = new HashSet<>();
        for (LineRequest line : lines) {
            if (!ids.add(line.productId())) throw new IllegalArgumentException("Duplicate product");
        }
        try (Connection c = database.connect()) {
            // Catalog reads share a repeatable snapshot; no catalog write permission is needed.
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            c.setAutoCommit(false);
            try {
                Map<String, BigDecimal> prices = new HashMap<>();
                BigDecimal total = new BigDecimal("0.00");
                for (LineRequest line : lines) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "SELECT unit_price FROM products WHERE product_id = ?")) {
                        ps.setString(1, line.productId());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new IllegalArgumentException("Unknown product");
                            BigDecimal price = rs.getBigDecimal(1);
                            prices.put(line.productId(), price);
                            total = total.add(price.multiply(BigDecimal.valueOf(line.quantity())));
                        }
                    }
                }
                if (total.compareTo(new BigDecimal("9999999999.99")) > 0)
                    throw new IllegalArgumentException("Order total exceeds supported limit");
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO orders (order_id, agency_id, description, total_amount, status) VALUES (?, ?, ?, ?, 'OPEN')")) {
                    ps.setString(1, id); ps.setString(2, actor.agencyId());
                    ps.setString(3, description); ps.setBigDecimal(4, total); ps.executeUpdate();
                }
                for (LineRequest line : lines) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)")) {
                        ps.setString(1, id); ps.setString(2, line.productId()); ps.setInt(3, line.quantity());
                        ps.setBigDecimal(4, prices.get(line.productId())); ps.executeUpdate();
                    }
                }
                audit(c, actor, "ORDER_CREATED", id);
                c.commit();
                return new Order(id, actor.agencyId(), description, total, "OPEN");
            } catch (SQLException | RuntimeException e) {
                rollback(c, e); throw e;
            }
        }
    }

    public List<Spending> spending(Actor actor) throws SQLException {
        Objects.requireNonNull(actor);
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement("""
                SELECT p.product_id, p.name, SUM(i.quantity) AS units,
                       SUM(i.quantity * i.unit_price) AS amount
                FROM orders o
                JOIN order_items i ON i.order_id = o.order_id
                JOIN products p ON p.product_id = i.product_id
                WHERE o.agency_id = ? AND o.status = 'OPEN'
                GROUP BY p.product_id, p.name
                ORDER BY p.product_id
                """)) {
            ps.setString(1, actor.agencyId());
            try (ResultSet rs = ps.executeQuery()) {
                List<Spending> result = new ArrayList<>();
                while (rs.next()) result.add(new Spending(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4)));
                return List.copyOf(result);
            }
        }
    }

    public boolean cancel(Actor actor, String id) throws SQLException {
        requireBuyer(actor);
        Actor.validateId(id);
        try (Connection c = database.connect()) {
            c.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ? AND agency_id = ? AND status = 'OPEN'")) {
                    ps.setString(1, id); ps.setString(2, actor.agencyId()); changed = ps.executeUpdate();
                }
                if (changed == 0) { c.rollback(); return false; }
                audit(c, actor, "ORDER_CANCELLED", id);
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) {
                rollback(c, e); throw e;
            }
        }
    }

    private static void requireBuyer(Actor actor) {
        Objects.requireNonNull(actor);
        if (actor.role() != Actor.Role.BUYER) throw new SecurityException("Buyer role required");
    }

    private static Order read(ResultSet rs) throws SQLException {
        return new Order(rs.getString("order_id"), rs.getString("agency_id"),
                rs.getString("description"), rs.getBigDecimal("total_amount"), rs.getString("status"));
    }

    private static void audit(Connection c, Actor actor, String action, String id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO audit_events (event_id, actor_id, agency_id, action, order_id) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, actor.id());
            ps.setString(3, actor.agencyId()); ps.setString(4, action); ps.setString(5, id); ps.executeUpdate();
        }
    }

    private static void rollback(Connection c, Exception original) {
        try { c.rollback(); } catch (SQLException rollbackFailure) { original.addSuppressed(rollbackFailure); }
    }
}
