# Five minute demonstration

Author: Justin Spratt

## With Docker Desktop

Start Docker Desktop with Linux containers enabled. From the repository folder:

```sh
docker compose up --build --abort-on-container-exit --exit-code-from verify verify
docker compose --profile demo up --build --abort-on-container-exit --exit-code-from demo demo
docker compose --profile demo down
```

The first command starts an isolated MySQL server and runs the full database suite. The second uses a separate MySQL database and a restricted application account to demonstrate real queries and writes. The final command removes the project's containers. Both databases are disposable and stored in memory; no database ports are published to your computer. Credentials in this configuration are public test fixtures, never production credentials.

The initial run downloads the official Java/Maven and MySQL images and Maven dependencies. Later runs reuse downloaded layers. The test runner exits with a failure if a test fails.

## Without Docker

Install JDK 21, set JAVA_HOME to its installation directory, then use the included Maven wrapper:

```powershell
.\mvnw.cmd verify exec:java
```

On macOS/Linux:

```sh
./mvnw verify exec:java
```

This path uses H2. It does not require Maven to be installed separately; the wrapper downloads Maven 3.9.12. Standard `mvn` commands also work when Maven is already installed.

## Explain what the audience sees

1. A fixed demo actor belongs to Agency A; it is a simulation, not a login.
2. Only Agency A's orders and spending appear. Agency B's order is hidden.
3. Two paint sets at 25.10 and three paper packs at 20.00 produce a total of **110.20**. Prices come from the database, not client input.
4. Cancellation changes the order and writes an audit event in one transaction.
5. An injection-shaped ID is rejected, and a viewer cannot cancel an order.
6. The tests also insert SQL-looking text into an ordinary description and prove it remains literal data.

The spending report measures **open order commitments**, not paid invoices or revenue. It excludes cancelled orders and uses the price captured when each item was ordered.

## Inspect the SQL

Read `schema.sql`, `seed.sql`, and `OrderService.spending`. The report joins orders, items, and products, filters the trusted agency and OPEN status, and groups by product. Discuss why the agency filter must apply before aggregation.
