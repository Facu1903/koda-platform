package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ModuleCapability(
    UUID id,
    String productCode,
    String code,
    String name,
    boolean enabled,
    boolean coreModule,
    boolean commerciallyToggleable,
    String entitlementStatus,
    Instant validFrom,
    Instant validUntil
) {

    public ModuleCapability {
        Objects.requireNonNull(id, "Module id is required");
        requireText(productCode, "Product code is required");
        requireText(code, "Module code is required");
        requireText(name, "Module name is required");
        requireText(entitlementStatus, "Module entitlement status is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}