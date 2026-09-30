package io.github.justinspratt.orders;

import java.util.Objects;

/** Trusted application context. This local demo does not authenticate users. */
public record Actor(String id, String agencyId, Role role) {
    public enum Role { BUYER, VIEWER }
    public Actor {
        validateId(id);
        validateId(agencyId);
        Objects.requireNonNull(role, "role");
    }
    static void validateId(String value) {
        if (value == null || !value.matches("[A-Z0-9-]{6,40}"))
            throw new IllegalArgumentException("IDs require 6-40 uppercase letters, digits or hyphens");
    }
}
