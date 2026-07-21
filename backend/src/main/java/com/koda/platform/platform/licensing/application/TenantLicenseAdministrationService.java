package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TenantLicenseAdministrationService {

    private static final Set<String> SUBSCRIPTION_STATUSES = Set.of("ACTIVE", "SUSPENDED", "EXPIRED", "CANCELLED");
    private static final Set<String> ENTITLEMENT_STATUSES = Set.of("ACTIVE", "SUSPENDED", "EXPIRED");

    private final TenantLicenseAdministrationRepository repository;
    private final TenantCapabilitiesCache capabilitiesCache;

    public TenantLicenseAdministrationService(TenantLicenseAdministrationRepository repository, TenantCapabilitiesCache capabilitiesCache) {
        this.repository = repository;
        this.capabilitiesCache = capabilitiesCache;
    }

    @Transactional(readOnly = true)
    public TenantLicenseAdministration getTenantLicenses(TenantId tenantId, PlatformLicenseAdminActor actor) {
        requirePlatformPermission(actor, PlatformLicenseAdminPermissions.READ);
        return findTenantLicenses(tenantId);
    }

    private TenantLicenseAdministration findTenantLicenses(TenantId tenantId) {
        TenantLicenseTenant tenant = repository.findTenant(tenantId)
            .orElseThrow(() -> new TenantLicenseAdminNotFoundException("tenant"));
        return repository.findAdministration(tenant);
    }

    @Transactional
    public TenantLicenseAdministration updateSubscription(
        TenantId tenantId,
        UUID subscriptionId,
        UpdateTenantProductSubscriptionCommand command,
        PlatformLicenseAdminActor actor,
        LicenseAdministrationRequestMetadata metadata
    ) {
        requirePlatformPermission(actor, PlatformLicenseAdminPermissions.UPDATE);
        TenantProductSubscriptionAdministration before = repository.findSubscription(tenantId, subscriptionId)
            .orElseThrow(() -> new TenantLicenseAdminNotFoundException("tenant_product_subscription"));
        UpdateTenantProductSubscriptionCommand normalized = normalizeSubscriptionCommand(command, before.validFrom());
        TenantProductSubscriptionAdministration after = repository.updateSubscription(tenantId, subscriptionId, actor.userId(), normalized)
            .orElseThrow(() -> new TenantLicenseAdminVersionConflictException("tenant_product_subscription"));
        audit(tenantId, actor.userId(), "license.subscription.update", "tenant_product_subscription", subscriptionId, metadata,
            beforeAfterDetails(before.status(), before.validUntil(), before.version(), after.status(), after.validUntil(), after.version()));
        evictCapabilitiesAfterCommit(tenantId);
        return findTenantLicenses(tenantId);
    }

    @Transactional
    public TenantLicenseAdministration updateProductEntitlement(
        TenantId tenantId,
        UUID entitlementId,
        UpdateTenantProductEntitlementCommand command,
        PlatformLicenseAdminActor actor,
        LicenseAdministrationRequestMetadata metadata
    ) {
        requirePlatformPermission(actor, PlatformLicenseAdminPermissions.UPDATE);
        TenantProductEntitlementAdministration before = repository.findProductEntitlement(tenantId, entitlementId)
            .orElseThrow(() -> new TenantLicenseAdminNotFoundException("tenant_product_entitlement"));
        UpdateTenantProductEntitlementCommand normalized = normalizeProductEntitlementCommand(command, before.validFrom());
        TenantProductEntitlementAdministration after = repository.updateProductEntitlement(tenantId, entitlementId, actor.userId(), normalized)
            .orElseThrow(() -> new TenantLicenseAdminVersionConflictException("tenant_product_entitlement"));
        audit(tenantId, actor.userId(), "license.product_entitlement.update", "tenant_product_entitlement", entitlementId, metadata,
            beforeAfterDetails(before.status(), before.validUntil(), before.version(), after.status(), after.validUntil(), after.version()));
        evictCapabilitiesAfterCommit(tenantId);
        return findTenantLicenses(tenantId);
    }

    @Transactional
    public TenantLicenseAdministration updateModuleEntitlement(
        TenantId tenantId,
        UUID entitlementId,
        UpdateTenantModuleEntitlementCommand command,
        PlatformLicenseAdminActor actor,
        LicenseAdministrationRequestMetadata metadata
    ) {
        requirePlatformPermission(actor, PlatformLicenseAdminPermissions.UPDATE);
        TenantModuleEntitlementAdministration before = repository.findModuleEntitlement(tenantId, entitlementId)
            .orElseThrow(() -> new TenantLicenseAdminNotFoundException("tenant_module_entitlement"));
        UpdateTenantModuleEntitlementCommand normalized = normalizeModuleEntitlementCommand(command, before);
        TenantModuleEntitlementAdministration after = repository.updateModuleEntitlement(tenantId, entitlementId, actor.userId(), normalized)
            .orElseThrow(() -> new TenantLicenseAdminVersionConflictException("tenant_module_entitlement"));
        audit(tenantId, actor.userId(), "license.module_entitlement.update", "tenant_module_entitlement", entitlementId, metadata,
            beforeAfterDetails(before.status(), before.validUntil(), before.version(), after.status(), after.validUntil(), after.version()));
        evictCapabilitiesAfterCommit(tenantId);
        return findTenantLicenses(tenantId);
    }

    private UpdateTenantProductSubscriptionCommand normalizeSubscriptionCommand(UpdateTenantProductSubscriptionCommand command, Instant validFrom) {
        requireVersion(command.version());
        String status = normalizeStatus(command.status(), SUBSCRIPTION_STATUSES, "INVALID_SUBSCRIPTION_STATUS");
        requireValidWindow(validFrom, command.validUntil());
        return new UpdateTenantProductSubscriptionCommand(command.version(), status, command.validUntil());
    }

    private UpdateTenantProductEntitlementCommand normalizeProductEntitlementCommand(UpdateTenantProductEntitlementCommand command, Instant validFrom) {
        requireVersion(command.version());
        String status = normalizeStatus(command.status(), ENTITLEMENT_STATUSES, "INVALID_ENTITLEMENT_STATUS");
        requireValidWindow(validFrom, command.validUntil());
        return new UpdateTenantProductEntitlementCommand(command.version(), status, command.validUntil());
    }

    private UpdateTenantModuleEntitlementCommand normalizeModuleEntitlementCommand(
        UpdateTenantModuleEntitlementCommand command,
        TenantModuleEntitlementAdministration before
    ) {
        requireVersion(command.version());
        String status = normalizeStatus(command.status(), ENTITLEMENT_STATUSES, "INVALID_ENTITLEMENT_STATUS");
        requireValidWindow(before.validFrom(), command.validUntil());
        if ((before.coreModule() || !before.commerciallyToggleable()) && (!"ACTIVE".equals(status) || command.validUntil() != null)) {
            throw new TenantLicenseAdminOperationRejectedException(before.coreModule() ? "CORE_MODULE_PROTECTED" : "MODULE_NOT_COMMERCIALLY_TOGGLEABLE");
        }
        return new UpdateTenantModuleEntitlementCommand(command.version(), status, command.validUntil());
    }

    private void requirePlatformPermission(PlatformLicenseAdminActor actor, String permission) {
        if (actor.platformAdmin() && actor.hasPermission(permission)) {
            return;
        }
        throw new PermissionDeniedException(permission);
    }

    private void requireVersion(long version) {
        if (version < 0) {
            throw new TenantLicenseAdminOperationRejectedException("INVALID_VERSION");
        }
    }

    private String normalizeStatus(String status, Set<String> allowedStatuses, String reasonCode) {
        if (status == null || status.isBlank()) {
            throw new TenantLicenseAdminOperationRejectedException(reasonCode);
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!allowedStatuses.contains(normalized)) {
            throw new TenantLicenseAdminOperationRejectedException(reasonCode);
        }
        return normalized;
    }

    private void requireValidWindow(Instant validFrom, Instant validUntil) {
        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new TenantLicenseAdminOperationRejectedException("INVALID_VALIDITY_WINDOW");
        }
    }

    private void evictCapabilitiesAfterCommit(TenantId tenantId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            capabilitiesCache.evict(tenantId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                capabilitiesCache.evict(tenantId);
            }
        });
    }

    private void audit(
        TenantId tenantId,
        UUID actorUserId,
        String action,
        String resourceType,
        UUID resourceId,
        LicenseAdministrationRequestMetadata metadata,
        Map<String, Object> details
    ) {
        repository.recordAuditEvent(tenantId, actorUserId, action, resourceType, resourceId, metadata, details);
    }

    private Map<String, Object> beforeAfterDetails(
        String beforeStatus,
        Instant beforeValidUntil,
        long beforeVersion,
        String afterStatus,
        Instant afterValidUntil,
        long afterVersion
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("auditSchemaVersion", 1);
        details.put("beforeStatus", beforeStatus);
        details.put("beforeValidUntil", beforeValidUntil);
        details.put("beforeVersion", beforeVersion);
        details.put("afterStatus", afterStatus);
        details.put("afterValidUntil", afterValidUntil);
        details.put("afterVersion", afterVersion);
        return details;
    }
}
