package com.koda.platform.platform.licensing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantCapabilitiesServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-20T18:00:00Z");

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private FakeCurrentTenantProvider currentTenantProvider;
    private FakeTenantCapabilitiesRepository repository;
    private TenantCapabilitiesService service;

    @BeforeEach
    void setUp() {
        currentTenantProvider = new FakeCurrentTenantProvider();
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("SALES_USER"), Set.of(), false));
        repository = new FakeTenantCapabilitiesRepository();
        service = new TenantCapabilitiesService(repository, currentTenantProvider, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void currentTenantCapabilitiesReturnsEffectiveProductsModulesLimitsAndFlags() {
        TenantCapabilities capabilities = service.currentTenantCapabilities();

        assertThat(capabilities.tenantId()).isEqualTo(tenantId);
        assertThat(capabilities.tenantActive()).isTrue();
        assertThat(capabilities.calculatedAt()).isEqualTo(FIXED_NOW);
        assertThat(capabilities.products()).hasSize(1);
        ProductCapability product = capabilities.products().getFirst();
        assertThat(product.code()).isEqualTo("KODA_ERP");
        assertThat(product.planCode()).isEqualTo("KODA_PILOT");
        assertThat(product.modules()).extracting(ModuleCapability::code).containsExactly("SALES", "STOCK");
        assertThat(capabilities.limits()).extracting(LimitCapability::code).containsExactly("MAX_USERS");
        assertThat(capabilities.featureFlags()).extracting(FeatureFlagCapability::code).containsExactly("NEW_NAVIGATION");
        assertThat(repository.lastCalculatedAt).isEqualTo(FIXED_NOW);
    }

    @Test
    void capabilitiesDoNotRequireBusinessPermissionBecauseTheyOnlyDescribeTenantAccess() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("READ_ONLY"), Set.of(), false));

        TenantCapabilities capabilities = service.currentTenantCapabilities();

        assertThat(capabilities.products()).hasSize(1);
    }

    @Test
    void inactiveTenantCannotResolveCapabilities() {
        repository.tenant = new TenantCapabilityTenant(tenantId, "SUSPENDED");

        assertThatThrownBy(() -> service.currentTenantCapabilities())
            .isInstanceOf(TenantCapabilitiesUnavailableException.class)
            .satisfies(exception -> assertThat(((TenantCapabilitiesUnavailableException) exception).reasonCode()).isEqualTo("TENANT_NOT_ACTIVE"));
    }

    private static class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private class FakeTenantCapabilitiesRepository implements TenantCapabilitiesRepository {
        private TenantCapabilityTenant tenant = new TenantCapabilityTenant(tenantId, "ACTIVE");
        private Instant lastCalculatedAt;

        @Override
        public Optional<TenantCapabilityTenant> findTenant(TenantId tenantId) {
            return Optional.ofNullable(tenant);
        }

        @Override
        public List<ProductCapability> findEnabledProducts(TenantId tenantId, Instant calculatedAt) {
            lastCalculatedAt = calculatedAt;
            return List.of(new ProductCapability(
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                "KODA_ERP",
                "KODA ERP",
                true,
                "ACTIVE",
                Instant.parse("2026-07-20T00:00:00Z"),
                null,
                UUID.fromString("22000000-0000-4000-8000-000000000001"),
                "ACTIVE",
                Instant.parse("2026-07-20T00:00:00Z"),
                null,
                "KODA_PILOT",
                "KODA Pilot",
                List.of()
            ));
        }

        @Override
        public List<ModuleCapability> findEnabledModules(TenantId tenantId, Instant calculatedAt) {
            return List.of(
                new ModuleCapability(UUID.fromString("10000000-0000-4000-8000-000000000108"), "KODA_ERP", "SALES", "Sales", true, false, true,
                    "ACTIVE", Instant.parse("2026-07-20T00:00:00Z"), null),
                new ModuleCapability(UUID.fromString("10000000-0000-4000-8000-000000000104"), "KODA_ERP", "STOCK", "Stock", true, false, true,
                    "ACTIVE", Instant.parse("2026-07-20T00:00:00Z"), null)
            );
        }

        @Override
        public List<FeatureFlagCapability> findEffectiveFeatureFlags(TenantId tenantId, Instant calculatedAt) {
            return List.of(new FeatureFlagCapability("KODA_ERP", null, "NEW_NAVIGATION", true, Instant.parse("2026-07-20T00:00:00Z"), null));
        }

        @Override
        public List<LimitCapability> findEffectiveLimits(TenantId tenantId, Instant calculatedAt) {
            return List.of(new LimitCapability("KODA_ERP", "MAX_USERS", null, true, "COUNT", "PLAN"));
        }
    }
}