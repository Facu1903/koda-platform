package com.koda.platform.platform.licensing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private FakeTenantCapabilitiesCache cache;
    private TenantCapabilitiesService service;

    @BeforeEach
    void setUp() {
        currentTenantProvider = new FakeCurrentTenantProvider();
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("SALES_USER"), Set.of(), false));
        repository = new FakeTenantCapabilitiesRepository();
        cache = new FakeTenantCapabilitiesCache();
        TenantCapabilitiesResolver resolver = new TenantCapabilitiesResolver(
            repository,
            cache,
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
            Duration.ofSeconds(30)
        );
        service = new TenantCapabilitiesService(resolver, currentTenantProvider);
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
    void currentTenantCapabilitiesUsesCachedSnapshotWithinTtl() {
        TenantCapabilities first = service.currentTenantCapabilities();
        TenantCapabilities second = service.currentTenantCapabilities();

        assertThat(second).isSameAs(first);
        assertThat(repository.findTenantCalls).isEqualTo(1);
        assertThat(repository.findEnabledProductsCalls).isEqualTo(1);
        assertThat(cache.lastExpiresAt).isEqualTo(FIXED_NOW.plusSeconds(30));
    }

    @Test
    void cacheExpirationUsesNearestCapabilityValidityWindow() {
        repository.moduleValidUntil = FIXED_NOW.plusSeconds(5);

        service.currentTenantCapabilities();

        assertThat(cache.lastExpiresAt).isEqualTo(FIXED_NOW.plusSeconds(5));
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

    private static class FakeTenantCapabilitiesCache implements TenantCapabilitiesCache {
        private final Map<TenantId, CacheEntry> entries = new HashMap<>();
        private Instant lastExpiresAt;

        @Override
        public Optional<TenantCapabilities> find(TenantId tenantId, Instant now) {
            CacheEntry entry = entries.get(tenantId);
            if (entry == null || !entry.expiresAt().isAfter(now)) {
                entries.remove(tenantId);
                return Optional.empty();
            }
            return Optional.of(entry.capabilities());
        }

        @Override
        public void put(TenantCapabilities capabilities, Instant now, Instant expiresAt) {
            lastExpiresAt = expiresAt;
            if (expiresAt.isAfter(now)) {
                entries.put(capabilities.tenantId(), new CacheEntry(capabilities, expiresAt));
            }
        }

        @Override
        public void evict(TenantId tenantId) {
            entries.remove(tenantId);
        }

        private record CacheEntry(TenantCapabilities capabilities, Instant expiresAt) {
        }
    }

    private class FakeTenantCapabilitiesRepository implements TenantCapabilitiesRepository {
        private TenantCapabilityTenant tenant = new TenantCapabilityTenant(tenantId, "ACTIVE");
        private Instant lastCalculatedAt;
        private Instant moduleValidUntil;
        private int findTenantCalls;
        private int findEnabledProductsCalls;

        @Override
        public Optional<TenantCapabilityTenant> findTenant(TenantId tenantId) {
            findTenantCalls++;
            return Optional.ofNullable(tenant);
        }

        @Override
        public List<ProductCapability> findEnabledProducts(TenantId tenantId, Instant calculatedAt) {
            findEnabledProductsCalls++;
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
                    "ACTIVE", Instant.parse("2026-07-20T00:00:00Z"), moduleValidUntil),
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