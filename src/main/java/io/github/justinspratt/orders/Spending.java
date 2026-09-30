package io.github.justinspratt.orders;

import java.math.BigDecimal;

/** Open-order commitments, not payment or revenue accounting. */
public record Spending(String productId, String productName, long units, BigDecimal amount) {}
