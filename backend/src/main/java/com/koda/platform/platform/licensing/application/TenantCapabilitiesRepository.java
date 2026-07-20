package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TenantCapabilitiesRepository {

    Optional<TenantCapabilityTenant> findTenant(TenantId tenantId);

    List<ProductCapability> findEnabledProducts(TenantId tenantId, Instant calculatedAt);

    List<ModuleCapability> findEnabledModules(TenantId tenantId, Instant calculatedAt);

    List<FeatureFlagCapability> findEffectiveFeatureFlags(TenantId tenantId, Instant calculatedAt);

    List<LimitCapability> findEffectiveLimits(TenantId tenantId, Instant calculatedAt);
}