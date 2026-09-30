package io.github.justinspratt.orders;

import java.util.List;
import java.util.UUID;

public final class Main {
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "demo" : args[0];
        if (!List.of("demo", "mysql-list", "mysql-demo").contains(command))
            throw new IllegalArgumentException("Use demo, mysql-list or mysql-demo");
        Database db = command.equals("demo") ? Database.demo() : Database.fromEnvironment();
        if (command.equals("demo")) db.initialize();
        OrderService service = new OrderService(db);
        Actor buyer = new Actor("BUYER-A", "AGENCY-A", Actor.Role.BUYER);
        System.out.println("Secure Agency Orders | Justin Spratt");
        System.out.println("Simulated actor: BUYER-A / AGENCY-A");
        System.out.println("Visible orders: " + service.list(buyer));
        System.out.println("Open-order spending: " + service.spending(buyer));
        if (command.equals("mysql-list")) return;
        System.out.println("Other agency order hidden: " + service.find(buyer, "ORDER-B200").isEmpty());
        String id = "ORD-" + UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
        Order created = service.create(buyer, id, "Workshop materials",
                List.of(new LineRequest("PROD-PAINT", 2), new LineRequest("PROD-PAPER", 3)));
        System.out.println("Created total: " + created.total() + " (2 x 25.10 + 3 x 20.00)");
        System.out.println("Cancelled: " + service.cancel(buyer, created.id()));
        try { service.find(buyer, "' OR '1'='1"); }
        catch (IllegalArgumentException expected) { System.out.println("Injection-shaped ID rejected"); }
        Actor viewer = new Actor("VIEWER-A", "AGENCY-A", Actor.Role.VIEWER);
        try { service.cancel(viewer, "ORDER-A100"); }
        catch (SecurityException expected) { System.out.println("Viewer write blocked"); }
        System.out.println("Creation and cancellation audit records committed with their changes.");
    }
}
