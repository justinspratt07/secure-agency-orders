package io.github.justinspratt.orders;

/** Quantity requested; price is always resolved from the catalog. */
public record LineRequest(String productId, int quantity) {
    public LineRequest {
        Actor.validateId(productId);
        if (quantity < 1 || quantity > 1000)
            throw new IllegalArgumentException("Quantity must be between 1 and 1000");
    }
}
