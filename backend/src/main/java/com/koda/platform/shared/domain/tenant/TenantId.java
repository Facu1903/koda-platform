package com.koda.platform.shared.domain.tenant;

import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "Tenant id is required");
    }

    public static TenantId from(UUID value) {
        return new TenantId(value);
    }

    public static TenantId fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tenant id is required");
        }
        return new TenantId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}