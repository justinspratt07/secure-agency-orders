package io.github.justinspratt.orders;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {
    Database db;
    OrderService service;
    Actor buyer = new Actor("BUYER-A", "AGENCY-A", Actor.Role.BUYER);
    Actor other = new Actor("BUYER-B", "AGENCY-B", Actor.Role.BUYER);
    Actor viewer = new Actor("VIEWER-A", "AGENCY-A", Actor.Role.VIEWER);

    @BeforeEach void setup() throws Exception {
        String url = System.getenv("TEST_DB_URL");
        if (url == null) {
            db = new Database("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        } else {
            // Destructive fixture cleanup is restricted to a dedicated, explicitly named test database.
            if (!url.matches("jdbc:mysql://[^/]+/orders_test(?:[?].*)?"))
                throw new IllegalArgumentException("TEST_DB_URL must name the disposable orders_test database");
            db = new Database(url, System.getenv("TEST_DB_USER"), System.getenv("TEST_DB_PASSWORD"));
            try (var c = db.connect(); var s = c.createStatement()) {
                s.execute("DROP TABLE IF EXISTS audit_events");
                s.execute("DROP TABLE IF EXISTS order_items");
                s.execute("DROP TABLE IF EXISTS orders");
                s.execute("DROP TABLE IF EXISTS products");
                s.execute("DROP TABLE IF EXISTS agencies");
            }
        }
        db.initialize(); service = new OrderService(db);
    }

    @Test void readsOnlyOwnAgency() throws Exception {
        assertEquals("ORDER-A100", service.list(buyer).getFirst().id());
        assertEquals(1, service.list(buyer).size());
        assertTrue(service.find(buyer, "ORDER-B200").isEmpty());
        assertTrue(service.find(buyer, "ORDER-MISSING").isEmpty());
        assertEquals("ORDER-B200", service.list(other).getFirst().id());
    }

    @ParameterizedTest @NullAndEmptySource
    @ValueSource(strings={"' OR '1'='1", "ORDER-A100; DROP TABLE orders", "abc", "ORDER_100"})
    void rejectsInvalidIds(String id) {
        assertThrows(IllegalArgumentException.class, () -> service.find(buyer, id));
    }

    @Test void bindsSqlLookingDescriptionAsLiteralData() throws Exception {
        String payload = "Supplies'); DROP TABLE orders; --";
        service.create(buyer, "ORDER-NEW1", payload, basket());
        assertEquals(payload, service.find(buyer, "ORDER-NEW1").orElseThrow().description());
        assertEquals(2, service.list(buyer).size());
        assertEquals(1, count("SELECT COUNT(*) FROM audit_events WHERE action = 'ORDER_CREATED'"));
    }

    @Test void viewerCanReadButCannotWrite() throws Exception {
        assertEquals(1, service.list(viewer).size());
        assertThrows(SecurityException.class, () -> service.create(viewer, "ORDER-NEW1", "Supplies", basket()));
        assertThrows(SecurityException.class, () -> service.cancel(viewer, "ORDER-A100"));
        assertEquals(0, count("SELECT COUNT(*) FROM audit_events"));
    }

    @Test void cancelIsAgencyScopedAndDoesNotRepeatAudit() throws Exception {
        assertFalse(service.cancel(buyer, "ORDER-B200"));
        assertEquals("OPEN", service.find(other, "ORDER-B200").orElseThrow().status());
        assertTrue(service.cancel(buyer, "ORDER-A100"));
        assertFalse(service.cancel(buyer, "ORDER-A100"));
        assertEquals("CANCELLED", service.find(buyer, "ORDER-A100").orElseThrow().status());
        assertEquals(1, count("SELECT COUNT(*) FROM audit_events WHERE actor_id = 'BUYER-A' AND agency_id = 'AGENCY-A' AND action = 'ORDER_CANCELLED'"));
    }

    private List<LineRequest> basket() { return List.of(new LineRequest("PROD-PAINT", 2), new LineRequest("PROD-PAPER", 3)); }

    @ParameterizedTest @ValueSource(ints={0, -1, 1001})
    void rejectsInvalidQuantity(int quantity) {
        assertThrows(IllegalArgumentException.class, () -> new LineRequest("PROD-PAINT", quantity));
    }

    @Test void calculatesTotalFromCatalogAndPreservesHistoricalPrices() throws Exception {
        Order created = service.create(buyer, "ORDER-NEW1", "Supplies", basket());
        assertEquals(new BigDecimal("110.20"), created.total());
        try (var c = db.connect(); var s = c.createStatement()) {
            s.executeUpdate("UPDATE products SET unit_price = 99.00 WHERE product_id = 'PROD-PAINT'");
        }
        assertEquals(new BigDecimal("110.20"), service.find(buyer, "ORDER-NEW1").orElseThrow().total());
        assertEquals(new BigDecimal("175.70"), service.spending(buyer).stream()
                .filter(x -> x.productId().equals("PROD-PAINT")).findFirst().orElseThrow().amount());
    }

    @Test void spendingExcludesOtherAgenciesAndCancelledOrders() throws Exception {
        assertEquals(List.of(new Spending("PROD-PAINT", "Classroom paint set", 5, new BigDecimal("125.50"))), service.spending(viewer));
        service.create(buyer, "ORDER-NEW1", "Supplies", basket());
        assertEquals(2, service.spending(buyer).size());
        service.cancel(buyer, "ORDER-NEW1");
        assertEquals(1, service.spending(buyer).size());
        service.cancel(buyer, "ORDER-A100");
        assertTrue(service.spending(buyer).isEmpty());
        assertEquals(new BigDecimal("80.00"), service.spending(other).getFirst().amount());
    }

    @Test void rejectsMissingAndDuplicateProductsWithoutPartialWrites() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> service.create(buyer, "ORDER-NEW1", "Supplies", List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.create(buyer, "ORDER-NEW1", "Supplies", List.of(new LineRequest("PROD-PAINT", 1), new LineRequest("PROD-PAINT", 2))));
        assertThrows(IllegalArgumentException.class, () -> service.create(buyer, "ORDER-NEW1", "Supplies", List.of(new LineRequest("PROD-PAINT", 1), new LineRequest("PROD-ZMISSING", 1))));
        assertTrue(service.find(buyer, "ORDER-NEW1").isEmpty());
        assertEquals(0, count("SELECT COUNT(*) FROM audit_events"));
        assertEquals(2, count("SELECT COUNT(*) FROM order_items"));
    }

    @Test void databaseRejectsInvalidStatusAndAmount() throws Exception {
        try (var c = db.connect(); var s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate("UPDATE orders SET status = 'INVALID' WHERE order_id = 'ORDER-A100'"));
            assertThrows(SQLException.class, () -> s.executeUpdate("UPDATE orders SET total_amount = 0 WHERE order_id = 'ORDER-A100'"));
        }
    }

    @Test void duplicateOrderDoesNotCreateAudit() throws Exception {
        assertThrows(SQLException.class, () -> service.create(buyer, "ORDER-A100", "Supplies", basket()));
        assertEquals(new BigDecimal("125.50"), service.find(buyer, "ORDER-A100").orElseThrow().total());
        assertEquals(0, count("SELECT COUNT(*) FROM audit_events"));
    }

    @Test void auditFailureRollsBackCreateAndCancel() throws Exception {
        try (var c = db.connect(); var s = c.createStatement()) { s.execute("DROP TABLE audit_events"); }
        assertThrows(SQLException.class, () -> service.create(buyer, "ORDER-NEW1", "Supplies", basket()));
        assertTrue(service.find(buyer, "ORDER-NEW1").isEmpty());
        assertEquals(2, count("SELECT COUNT(*) FROM order_items"));
        assertThrows(SQLException.class, () -> service.cancel(buyer, "ORDER-A100"));
        assertEquals("OPEN", service.find(buyer, "ORDER-A100").orElseThrow().status());
    }

    @Test void rejectsBadDescriptionAndUnknownAgency() {
        assertThrows(IllegalArgumentException.class, () -> service.create(buyer, "ORDER-NEW1", "\n", basket()));
        assertThrows(IllegalArgumentException.class, () -> service.create(buyer, "ORDER-NEW1", "x".repeat(201), basket()));
        Actor unknown = new Actor("BUYER-X", "AGENCY-X", Actor.Role.BUYER);
        assertThrows(SQLException.class, () -> service.create(unknown, "ORDER-NEW1", "Supplies", basket()));
    }

    private int count(String sql) throws SQLException {
        try (var c = db.connect(); var s = c.createStatement(); var rs = s.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        }
    }
}
