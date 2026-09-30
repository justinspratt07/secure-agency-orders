# Software assurance evidence

Author: Justin Spratt

The coursework proposed a broad assurance program. This implementation makes a narrower set of controls executable and testable.

| Risk | Implemented control | Evidence |

| --- | --- | --- |

| SQL injection | Bound query parameters and ID validation | Literal SQL-looking description survives unchanged; malformed IDs are rejected |

| Cross-agency disclosure or modification | Agency predicates on reads and cancellation | Other agency order cannot be found or cancelled |

| Unauthorized writes by viewers | Buyer role check before mutation | Viewer reads succeed and writes fail |

| Incomplete audit trail after a failed write | Audit and mutation share one transaction | Forced audit failure rolls back both create and cancel |

| Invalid financial data | Exact decimals and positive amount validation | Database rejects nonpositive amounts; quantities are bounded; totals come from catalog prices |

| Repeated cancellation | Conditional OPEN-to-CANCELLED update | Only the first cancellation adds an event |

| Orphaned or duplicated records | Foreign keys and primary keys | Unknown agency and duplicate-order tests |

| Incorrect totals or changed historical prices | Catalog pricing and item price snapshots | Exact basket total and catalog-price-change tests |
| Cross-agency reporting | Agency filter before aggregation | Report excludes other agencies and cancelled orders |

## CI scope

The workflow runs the suite on H2 and on a MySQL service, then exercises the Compose tests and restricted-account demo. Dependabot configuration proposes dependency updates. Neither constitutes a full vulnerability assessment. The illustrative SAST, secret scanning, SBOM, and release-policy commands from the coursework are not claimed as implemented here.

## Next implementation steps

Add verified authentication before exposing an API, version database migrations, record denied actions without sensitive inputs, and introduce pagination. Configure repository branch protection and review requirements after publication; a workflow file cannot enable those settings by itself.
