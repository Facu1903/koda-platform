package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Objects;

public record TenantCapabilityTenant(TenantId tenantId, String status) {

    public TenantCapabilityTenant {
        Objects.requireNonNull(tenantId, "Tenant id is required");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Tenant status is required");
        }
    }

    public boolean active() {
        return "ACTIVE".equals(status);
    }
}