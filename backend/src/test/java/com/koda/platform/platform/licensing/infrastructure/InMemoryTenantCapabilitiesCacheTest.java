package com.koda.platform.platform.licensing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.koda.platform.platform.licensing.application.TenantCapabilities;
import com.koda.platform.shared.domain.tenant.TenantId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryTenantCapabilitiesCacheTest {

    private static final Instant NOW = Instant.parse("2026-07-21T19:30:00Z");
    private static final TenantId TENANT_ID = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));

    @Test
    void returnsCachedCapabilitiesBeforeExpiration() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InMemoryTenantCapabilitiesCache cache = new InMemoryTenantCapabilitiesCache(true, 10, registry);
        TenantCapabilities capabilities = capabilities(TENANT_ID);

        cache.put(capabilities, NOW, NOW.plusSeconds(30));

        assertThat(cache.find(TENANT_ID, NOW.plusSeconds(10))).containsSame(capabilities);
        assertThat(registry.find("koda.capabilities.cache.requests").tag("result", "hit").counter().count()).isEqualTo(1);
    }

    @Test
    void evictsExpiredCapabilities() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InMemoryTenantCapabilitiesCache cache = new InMemoryTenantCapabilitiesCache(true, 10, registry);
        cache.put(capabilities(TENANT_ID), NOW, NOW.plusSeconds(30));

        assertThat(cache.find(TENANT_ID, NOW.plusSeconds(31))).isEmpty();
        assertThat(registry.find("koda.capabilities.cache.requests").tag("result", "expired").counter().count()).isEqualTo(1);
    }

    @Test
    void explicitEvictionRemovesTenantCapabilities() {
        InMemoryTenantCapabilitiesCache cache = new InMemoryTenantCapabilitiesCache(true, 10, new SimpleMeterRegistry());
        cache.put(capabilities(TENANT_ID), NOW, NOW.plusSeconds(30));

        cache.evict(TENANT_ID);

        assertThat(cache.find(TENANT_ID, NOW.plusSeconds(10))).isEmpty();
    }

    @Test
    void disabledCacheDoesNotStoreCapabilities() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InMemoryTenantCapabilitiesCache cache = new InMemoryTenantCapabilitiesCache(false, 10, registry);
        cache.put(capabilities(TENANT_ID), NOW, NOW.plusSeconds(30));

        assertThat(cache.find(TENANT_ID, NOW.plusSeconds(10))).isEmpty();
        assertThat(registry.find("koda.capabilities.cache.requests").tag("result", "skipped").counter().count()).isEqualTo(2);
    }

    private TenantCapabilities capabilities(TenantId tenantId) {
        return new TenantCapabilities(tenantId, true, NOW, List.of(), List.of(), List.of());
    }
}

