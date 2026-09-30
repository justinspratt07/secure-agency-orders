package io.github.justinspratt.orders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public record Database(String url, String user, String password) {
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static Database demo() {
        return new Database("jdbc:h2:mem:orders;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    public static Database fromEnvironment() {
        String url = required("DB_URL");
        if (!url.startsWith("jdbc:mysql:")) {
            throw new IllegalArgumentException("DB_URL must be a MySQL JDBC URL");
        }
        return new Database(url, required("DB_USER"), required("DB_PASSWORD"));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
        return value;
    }

    // Only bundled, trusted migration/fixture SQL is executed as a script.
    public void initialize() throws SQLException, IOException {
        try (Connection c = connect()) {
            script(c, "/schema.sql");
            script(c, "/seed.sql");
        }
    }

    private static void script(Connection c, String resource) throws SQLException, IOException {
        try (var input = Database.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing SQL resource");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                if (!statement.isBlank()) {
                    try (Statement s = c.createStatement()) { s.execute(statement); }
                }
            }
        }
    }

    // Do not let the generated record representation expose database credentials.
    @Override public String toString() { return "Database[configuration redacted]"; }
}
