package io.github.justinspratt.orders;

import java.math.BigDecimal;

public record Order(String id, String agencyId, String description, BigDecimal total, String status) {}
