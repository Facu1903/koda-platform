package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Optional;

public interface TenantCapabilitiesCache {

    Optional<TenantCapabilities> find(TenantId tenantId, Instant now);

    void put(TenantCapabilities capabilities, Instant now, Instant expiresAt);

    void evict(TenantId tenantId);
}

