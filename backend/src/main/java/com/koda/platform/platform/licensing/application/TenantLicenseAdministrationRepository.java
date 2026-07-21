package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TenantLicenseAdministrationRepository {

    Optional<TenantLicenseTenant> findTenant(TenantId tenantId);

    TenantLicenseAdministration findAdministration(TenantLicenseTenant tenant);

    Optional<TenantProductSubscriptionAdministration> findSubscription(TenantId tenantId, UUID subscriptionId);

    Optional<TenantProductSubscriptionAdministration> updateSubscription(
        TenantId tenantId,
        UUID subscriptionId,
        UUID actorUserId,
        UpdateTenantProductSubscriptionCommand command
    );

    Optional<TenantProductEntitlementAdministration> findProductEntitlement(TenantId tenantId, UUID entitlementId);

    Optional<TenantProductEntitlementAdministration> updateProductEntitlement(
        TenantId tenantId,
        UUID entitlementId,
        UUID actorUserId,
        UpdateTenantProductEntitlementCommand command
    );

    Optional<TenantModuleEntitlementAdministration> findModuleEntitlement(TenantId tenantId, UUID entitlementId);

    Optional<TenantModuleEntitlementAdministration> updateModuleEntitlement(
        TenantId tenantId,
        UUID entitlementId,
        UUID actorUserId,
        UpdateTenantModuleEntitlementCommand command
    );

    void recordAuditEvent(
        TenantId tenantId,
        UUID actorUserId,
        String action,
        String resourceType,
        UUID resourceId,
        LicenseAdministrationRequestMetadata metadata,
        Map<String, Object> details
    );
}
