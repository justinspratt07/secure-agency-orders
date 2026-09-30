# Design discussion guide

Author: Justin Spratt

Use these prompts to practice explaining the implementation. They are study prompts, not claims about personal learning or work experience.

| Question | Point to demonstrate in the code |
| --- | --- |
| Why prepared statements? | Values are bound separately from SQL syntax, including free-text descriptions. |
| Why validate an ID as well? | Validation enforces the application's format; binding is still required for injection resistance. |
| Why put agency ID in SQL? | Knowing another order ID must not reveal or modify another agency's data. |
| Why store the unit price on each item? | Historical totals must not change when the catalog price changes. |
| Why use BigDecimal? | Currency arithmetic needs exact decimals and predictable precision. |
| Why one transaction? | Header, items and audit must all succeed or all roll back. |
| Why lock product rows? | Prices remain stable while the basket is priced and written. Stable lock ordering reduces deadlock risk. |
| Why test both H2 and MySQL? | Compatibility mode does not guarantee identical SQL or transaction behavior. |
| What does the report mean? | Open-order commitments per product for one agency, not money actually paid. |
| What remains before public deployment? | Authentication, trusted role/agency resolution, migrations, pagination, operational logging and deployment controls. |

Before sharing the project in an interview, run the demo yourself, explain a rollback test, and make a small change you can describe confidently.
