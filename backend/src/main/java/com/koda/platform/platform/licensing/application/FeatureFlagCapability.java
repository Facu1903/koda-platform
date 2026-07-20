package com.koda.platform.platform.licensing.application;

import java.time.Instant;

public record FeatureFlagCapability(
    String productCode,
    String moduleCode,
    String code,
    boolean enabled,
    Instant validFrom,
    Instant validUntil
) {

    public FeatureFlagCapability {
        requireText(productCode, "Product code is required");
        requireText(code, "Feature flag code is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}