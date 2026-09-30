# Verification record

Author: Justin Spratt

## Verified implementation

[GitHub Actions run 36653759431](https://github.com/justinspratt07/secure-agency-orders/actions/runs/36653759431) passed all three jobs for commit `9bedefb7d86216280924bbb46aab2be6a77fc47a` on September 30, 2026 UTC (September 29 in US Eastern time).

| Environment | Result |
| --- | --- |
| H2 on Java 21, using the Maven wrapper | 20 tests passed; CLI demo passed |
| MySQL 8.4 service on GitHub Actions | 20 tests passed, no failures, errors or skips |
| Docker Compose with real MySQL | 20 tests passed; restricted-account MySQL demo passed |

The container demo used the exact commands documented in the README. It verified a **110.20** total from catalog prices, cancellation, hidden cross-agency orders, rejected injection-shaped IDs, and blocked viewer writes. The application account has SELECT-only access to products.

Local Windows verification also passed the 20-test suite using Java 21.0.10 and Maven 3.9.12. The separate Windows wrapper bootstrap was not executed because the execution approval service was temporarily unavailable. The wrapper was exercised successfully on Linux in GitHub Actions.

## What testing caught

- An H2 2.4.240 status-check evaluation issue after the schema-creating connection closed. The equivalent CASE constraint passes and still rejects invalid statuses.
- MySQL's extra permission requirement for the original locking catalog query. Pricing now uses repeatable-read isolation with ordinary SELECT, retaining read-only catalog permissions.
- Formatting in the first workflow's service health-check options. The corrected service starts and the MySQL job passes.

## Scope

These are behavior and integration checks, not a penetration test or security certification. Identities remain simulated. No production deployment, login flow, or public API is provided. See the architecture and assurance notes for the trust boundary.
