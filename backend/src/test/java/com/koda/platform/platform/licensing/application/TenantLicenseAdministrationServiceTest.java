package com.koda.platform.platform.licensing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantLicenseAdministrationServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID actorUserId = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private final UUID subscriptionId = UUID.fromString("22000000-0000-4000-8000-000000000001");
    private final UUID productEntitlementId = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private final UUID moduleEntitlementId = UUID.fromString("20000000-0000-4000-8000-000000000108");
    private FakeTenantLicenseAdministrationRepository repository;
    private FakeTenantCapabilitiesCache capabilitiesCache;
    private TenantLicenseAdministrationService service;

    @BeforeEach
    void setUp() {
        repository = new FakeTenantLicenseAdministrationRepository();
        capabilitiesCache = new FakeTenantCapabilitiesCache();
        service = new TenantLicenseAdministrationService(repository, capabilitiesCache);
    }

    @Test
    void readRequiresPlatformAdministratorWithLicenseReadPermission() {
        PlatformLicenseAdminActor tenantAdmin = new PlatformLicenseAdminActor(actorUserId, Set.of(PlatformLicenseAdminPermissions.READ), false);

        assertThatThrownBy(() -> service.getTenantLicenses(tenantId, tenantAdmin))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).requiredPermission())
                .isEqualTo(PlatformLicenseAdminPermissions.READ));
    }

    @Test
    void updateSubscriptionNormalizesStatusUsesOptimisticVersionAuditsAndEvictsCache() {
        PlatformLicenseAdminActor actor = platformActor(PlatformLicenseAdminPermissions.UPDATE);
        Instant validUntil = Instant.parse("2026-12-31T23:59:59Z");

        TenantLicenseAdministration administration = service.updateSubscription(
            tenantId,
            subscriptionId,
            new UpdateTenantProductSubscriptionCommand(0, "suspended", validUntil),
            actor,
            new LicenseAdministrationRequestMetadata("127.0.0.1", "JUnit")
        );

        assertThat(repository.updatedSubscriptionCommand.status()).isEqualTo("SUSPENDED");
        assertThat(repository.updatedSubscriptionCommand.version()).isZero();
        assertThat(repository.subscription.status()).isEqualTo("SUSPENDED");
        assertThat(repository.auditAction).isEqualTo("license.subscription.update");
        assertThat(repository.auditDetails).containsEntry("beforeStatus", "ACTIVE").containsEntry("afterStatus", "SUSPENDED");
        assertThat(administration.subscriptions()).hasSize(1);
        assertThat(capabilitiesCache.evictedTenantIds).containsExactly(tenantId);
    }

    @Test
    void staleSubscriptionVersionIsRejectedAsConflictAndDoesNotEvictCache() {
        repository.failSubscriptionUpdate = true;

        assertThatThrownBy(() -> service.updateSubscription(
            tenantId,
            subscriptionId,
            new UpdateTenantProductSubscriptionCommand(99, "ACTIVE", null),
            platformActor(PlatformLicenseAdminPermissions.UPDATE),
            null
        )).isInstanceOf(TenantLicenseAdminVersionConflictException.class)
            .satisfies(exception -> assertThat(((TenantLicenseAdminVersionConflictException) exception).resource())
                .isEqualTo("tenant_product_subscription"));
        assertThat(capabilitiesCache.evictedTenantIds).isEmpty();
    }

    @Test
    void coreModuleEntitlementCannotBeSuspended() {
        repository.moduleEntitlement = new TenantModuleEntitlementAdministration(
            moduleEntitlementId,
            UUID.fromString("10000000-0000-4000-8000-000000000001"),
            "KODA_ERP",
            "KODA ERP",
            UUID.fromString("10000000-0000-4000-8000-000000000101"),
            "SECURITY",
            "Security",
            true,
            false,
            "ACTIVE",
            Instant.parse("2026-07-20T00:00:00Z"),
            null,
            0,
            Instant.parse("2026-07-20T00:00:00Z")
        );

        assertThatThrownBy(() -> service.updateModuleEntitlement(
            tenantId,
            moduleEntitlementId,
            new UpdateTenantModuleEntitlementCommand(0, "SUSPENDED", null),
            platformActor(PlatformLicenseAdminPermissions.UPDATE),
            null
        )).isInstanceOf(TenantLicenseAdminOperationRejectedException.class)
            .satisfies(exception -> assertThat(((TenantLicenseAdminOperationRejectedException) exception).reasonCode())
                .isEqualTo("CORE_MODULE_PROTECTED"));
        assertThat(capabilitiesCache.evictedTenantIds).isEmpty();
    }

    @Test
    void productEntitlementUpdateUsesDedicatedAuditActionAndEvictsCache() {
        service.updateProductEntitlement(
            tenantId,
            productEntitlementId,
            new UpdateTenantProductEntitlementCommand(0, "EXPIRED", null),
            platformActor(PlatformLicenseAdminPermissions.UPDATE),
            null
        );

        assertThat(repository.productEntitlement.status()).isEqualTo("EXPIRED");
        assertThat(repository.auditAction).isEqualTo("license.product_entitlement.update");
        assertThat(repository.auditResourceType).isEqualTo("tenant_product_entitlement");
        assertThat(capabilitiesCache.evictedTenantIds).containsExactly(tenantId);
    }

    @Test
    void moduleEntitlementUpdateEvictsCapabilitiesCache() {
        service.updateModuleEntitlement(
            tenantId,
            moduleEntitlementId,
            new UpdateTenantModuleEntitlementCommand(0, "SUSPENDED", null),
            platformActor(PlatformLicenseAdminPermissions.UPDATE),
            null
        );

        assertThat(repository.moduleEntitlement.status()).isEqualTo("SUSPENDED");
        assertThat(repository.auditAction).isEqualTo("license.module_entitlement.update");
        assertThat(capabilitiesCache.evictedTenantIds).containsExactly(tenantId);
    }

    private PlatformLicenseAdminActor platformActor(String permission) {
        return new PlatformLicenseAdminActor(actorUserId, Set.of(permission), true);
    }

    private static final class FakeTenantCapabilitiesCache implements TenantCapabilitiesCache {
        private final List<TenantId> evictedTenantIds = new ArrayList<>();

        @Override
        public Optional<TenantCapabilities> find(TenantId tenantId, Instant now) {
            return Optional.empty();
        }

        @Override
        public void put(TenantCapabilities capabilities, Instant now, Instant expiresAt) {
        }

        @Override
        public void evict(TenantId tenantId) {
            evictedTenantIds.add(tenantId);
        }
    }

    private final class FakeTenantLicenseAdministrationRepository implements TenantLicenseAdministrationRepository {

        private final TenantLicenseTenant tenant = new TenantLicenseTenant(
            tenantId,
            "KODA",
            "KODA",
            "ACTIVE",
            "AR",
            "es-AR",
            "ARS",
            "America/Argentina/Buenos_Aires",
            0
        );
        private TenantProductSubscriptionAdministration subscription = subscription("ACTIVE", 0);
        private TenantProductEntitlementAdministration productEntitlement = productEntitlement("ACTIVE", 0);
        private TenantModuleEntitlementAdministration moduleEntitlement = moduleEntitlement("ACTIVE", 0);
        private UpdateTenantProductSubscriptionCommand updatedSubscriptionCommand;
        private boolean failSubscriptionUpdate;
        private String auditAction;
        private String auditResourceType;
        private Map<String, Object> auditDetails = Map.of();

        @Override
        public Optional<TenantLicenseTenant> findTenant(TenantId tenantId) {
            return Optional.of(tenant);
        }

        @Override
        public TenantLicenseAdministration findAdministration(TenantLicenseTenant tenant) {
            return new TenantLicenseAdministration(
                tenant,
                Instant.parse("2026-07-21T00:00:00Z"),
                List.of(subscription),
                List.of(productEntitlement),
                List.of(moduleEntitlement),
                List.of(),
                List.of()
            );
        }

        @Override
        public Optional<TenantProductSubscriptionAdministration> findSubscription(TenantId tenantId, UUID subscriptionId) {
            return Optional.of(subscription);
        }

        @Override
        public Optional<TenantProductSubscriptionAdministration> updateSubscription(
            TenantId tenantId,
            UUID subscriptionId,
            UUID actorUserId,
            UpdateTenantProductSubscriptionCommand command
        ) {
            updatedSubscriptionCommand = command;
            if (failSubscriptionUpdate) {
                return Optional.empty();
            }
            subscription = subscription(command.status(), subscription.version() + 1);
            return Optional.of(subscription);
        }

        @Override
        public Optional<TenantProductEntitlementAdministration> findProductEntitlement(TenantId tenantId, UUID entitlementId) {
            return Optional.of(productEntitlement);
        }

        @Override
        public Optional<TenantProductEntitlementAdministration> updateProductEntitlement(
            TenantId tenantId,
            UUID entitlementId,
            UUID actorUserId,
            UpdateTenantProductEntitlementCommand command
        ) {
            productEntitlement = productEntitlement(command.status(), productEntitlement.version() + 1);
            return Optional.of(productEntitlement);
        }

        @Override
        public Optional<TenantModuleEntitlementAdministration> findModuleEntitlement(TenantId tenantId, UUID entitlementId) {
            return Optional.of(moduleEntitlement);
        }

        @Override
        public Optional<TenantModuleEntitlementAdministration> updateModuleEntitlement(
            TenantId tenantId,
            UUID entitlementId,
            UUID actorUserId,
            UpdateTenantModuleEntitlementCommand command
        ) {
            moduleEntitlement = moduleEntitlement(command.status(), moduleEntitlement.version() + 1);
            return Optional.of(moduleEntitlement);
        }

        @Override
        public void recordAuditEvent(
            TenantId tenantId,
            UUID actorUserId,
            String action,
            String resourceType,
            UUID resourceId,
            LicenseAdministrationRequestMetadata metadata,
            Map<String, Object> details
        ) {
            auditAction = action;
            auditResourceType = resourceType;
            auditDetails = details;
        }

        private TenantProductSubscriptionAdministration subscription(String status, long version) {
            return new TenantProductSubscriptionAdministration(
                subscriptionId,
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "KODA_ERP",
                "KODA ERP",
                UUID.fromString("21000000-0000-4000-8000-000000000001"),
                "KODA_PILOT",
                "KODA Pilot",
                status,
                Instant.parse("2026-07-20T00:00:00Z"),
                null,
                "PILOT",
                null,
                version,
                Instant.parse("2026-07-20T00:00:00Z")
            );
        }

        private TenantProductEntitlementAdministration productEntitlement(String status, long version) {
            return new TenantProductEntitlementAdministration(
                productEntitlementId,
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "KODA_ERP",
                "KODA ERP",
                status,
                Instant.parse("2026-07-20T00:00:00Z"),
                null,
                version,
                Instant.parse("2026-07-20T00:00:00Z")
            );
        }

        private TenantModuleEntitlementAdministration moduleEntitlement(String status, long version) {
            return new TenantModuleEntitlementAdministration(
                moduleEntitlementId,
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "KODA_ERP",
                "KODA ERP",
                UUID.fromString("10000000-0000-4000-8000-000000000108"),
                "SALES",
                "Sales",
                false,
                true,
                status,
                Instant.parse("2026-07-20T00:00:00Z"),
                null,
                version,
                Instant.parse("2026-07-20T00:00:00Z")
            );
        }
    }
}