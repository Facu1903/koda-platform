package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.List;

public record TenantLicenseAdministration(
    TenantLicenseTenant tenant,
    Instant retrievedAt,
    List<TenantProductSubscriptionAdministration> subscriptions,
    List<TenantProductEntitlementAdministration> productEntitlements,
    List<TenantModuleEntitlementAdministration> moduleEntitlements,
    List<TenantLimitOverrideAdministration> limitOverrides,
    List<TenantFeatureFlagAdministration> featureFlags
) {

    public TenantLicenseAdministration {
        subscriptions = List.copyOf(subscriptions);
        productEntitlements = List.copyOf(productEntitlements);
        moduleEntitlements = List.copyOf(moduleEntitlements);
        limitOverrides = List.copyOf(limitOverrides);
        featureFlags = List.copyOf(featureFlags);
    }
}
