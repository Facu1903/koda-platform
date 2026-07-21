package com.koda.platform.platform.licensing.application;

import java.time.Instant;

public record LimitCapability(
    String productCode,
    String code,
    Long value,
    boolean unlimited,
    String unit,
    String source,
    Instant validFrom,
    Instant validUntil
) {

    public LimitCapability(String productCode, String code, Long value, boolean unlimited, String unit, String source) {
        this(productCode, code, value, unlimited, unit, source, null, null);
    }

    public LimitCapability {
        requireText(productCode, "Product code is required");
        requireText(code, "Limit code is required");
        requireText(unit, "Limit unit is required");
        requireText(source, "Limit source is required");
        if (!unlimited && value == null) {
            throw new IllegalArgumentException("Limit value is required when limit is not unlimited");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}