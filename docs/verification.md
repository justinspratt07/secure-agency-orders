# Verification record

Author: Justin Spratt

Local verification on September 29, 2026: Java 21, Maven 3.9.12, H2 2.4.240. The expanded database suite passes 20 tests with no failures, errors, or skips. The demo verifies a 110.20 calculated basket total, cancellation, blocked viewer writes and hidden cross-agency orders.

MySQL and Compose results will be linked here after the first GitHub Actions run. A workflow definition is not evidence that a job has passed.

The status constraint uses CASE because the equivalent IN expression in H2 2.4.240 failed after the schema-creating connection closed. The suite checks that the constraint rejects an invalid status using another connection.
