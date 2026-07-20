package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TenantCapabilities(
    TenantId tenantId,
    boolean tenantActive,
    Instant calculatedAt,
    List<ProductCapability> products,
    List<FeatureFlagCapability> featureFlags,
    List<LimitCapability> limits
) {

    public TenantCapabilities {
        Objects.requireNonNull(tenantId, "Tenant id is required");
        Objects.requireNonNull(calculatedAt, "Calculation timestamp is required");
        products = products == null ? List.of() : List.copyOf(products);
        featureFlags = featureFlags == null ? List.of() : List.copyOf(featureFlags);
        limits = limits == null ? List.of() : List.copyOf(limits);
    }
}