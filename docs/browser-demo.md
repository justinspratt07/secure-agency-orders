# Browser demo

Author: Justin Spratt

[Launch the demo](https://justinspratt07.github.io/secure-agency-orders/).

1. Start as Cedar School District / Buyer. The sample open order totals $125.50.
2. Create the default basket: 2 paint sets and 3 paper packs total $110.20. The order, commitments and audit trail update together.
3. Cancel the new order. It remains visible as Cancelled and disappears from open commitments.
4. Switch to Maple Public Library to see its separate $80.00 sample order.
5. Switch to Viewer. Create and cancel controls are disabled.
6. Reset demo to return to the original fixtures.

## Scope

This dependency-free static frontend is an interactive illustration of the Java application's workflow. It does not connect to a database, send orders to a server, store browser data, or authenticate users. All state is held in memory. Anyone can inspect or alter the frontend; simulated roles and agency filtering are not security boundaries. Backend authorization, prepared statements and transactional behavior are independently covered by the Java/H2/MySQL tests.

Prices use integer cents and line-item snapshots. Descriptions render as text, never as HTML. Quantities must be whole numbers between 0 and 1,000 with at least one product. Audit entries represent successful simulated mutations only.

## Development and deployment

Serve the `demo` directory with any static HTTP server. No build or package install is required. Run `node --test tests/demo.test.mjs` with Node 22 or newer to verify the simulation. The Browser demo workflow runs these tests on pull requests and deploys only `demo/` to GitHub Pages after a successful main-branch run. Configure the repository's Pages source as GitHub Actions.

The site uses semantic forms, labeled controls, keyboard focus outlines, live feedback, and responsive panels. On narrow screens the orders table scrolls within its labeled region.
