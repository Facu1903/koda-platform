package com.koda.platform.platform.licensing.infrastructure;

import com.koda.platform.platform.licensing.application.TenantCapabilities;
import com.koda.platform.platform.licensing.application.TenantCapabilitiesCache;
import com.koda.platform.shared.domain.tenant.TenantId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryTenantCapabilitiesCache implements TenantCapabilitiesCache {

    private final ConcurrentMap<TenantId, CacheEntry> entries = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int maxSize;
    private final Counter hits;
    private final Counter misses;
    private final Counter expired;
    private final Counter evicted;
    private final Counter skipped;

    public InMemoryTenantCapabilitiesCache(
        @Value("${koda.licensing.capabilities-cache.enabled:true}") boolean enabled,
        @Value("${koda.licensing.capabilities-cache.max-size:10000}") int maxSize,
        MeterRegistry meterRegistry
    ) {
        this.enabled = enabled;
        this.maxSize = maxSize;
        this.hits = counter(meterRegistry, "hit");
        this.misses = counter(meterRegistry, "miss");
        this.expired = counter(meterRegistry, "expired");
        this.evicted = counter(meterRegistry, "evicted");
        this.skipped = counter(meterRegistry, "skipped");
    }

    @Override
    public Optional<TenantCapabilities> find(TenantId tenantId, Instant now) {
        Objects.requireNonNull(tenantId, "Tenant id is required");
        Objects.requireNonNull(now, "Lookup timestamp is required");
        if (!enabled || maxSize <= 0) {
            skipped.increment();
            return Optional.empty();
        }
        CacheEntry entry = entries.get(tenantId);
        if (entry == null) {
            misses.increment();
            return Optional.empty();
        }
        if (entry.expiresAt().isAfter(now)) {
            hits.increment();
            return Optional.of(entry.capabilities());
        }
        entries.remove(tenantId, entry);
        expired.increment();
        return Optional.empty();
    }

    @Override
    public void put(TenantCapabilities capabilities, Instant now, Instant expiresAt) {
        Objects.requireNonNull(capabilities, "Tenant capabilities are required");
        Objects.requireNonNull(now, "Cache write timestamp is required");
        Objects.requireNonNull(expiresAt, "Cache expiration timestamp is required");
        if (!enabled || maxSize <= 0 || !expiresAt.isAfter(now)) {
            skipped.increment();
            return;
        }
        if (!entries.containsKey(capabilities.tenantId()) && entries.size() >= maxSize) {
            evictExpired(now);
        }
        if (!entries.containsKey(capabilities.tenantId()) && entries.size() >= maxSize) {
            evictOne();
        }
        entries.put(capabilities.tenantId(), new CacheEntry(capabilities, expiresAt));
    }

    @Override
    public void evict(TenantId tenantId) {
        Objects.requireNonNull(tenantId, "Tenant id is required");
        if (entries.remove(tenantId) != null) {
            evicted.increment();
        }
    }

    private void evictExpired(Instant now) {
        entries.entrySet().removeIf(entry -> {
            boolean shouldRemove = !entry.getValue().expiresAt().isAfter(now);
            if (shouldRemove) {
                expired.increment();
            }
            return shouldRemove;
        });
    }

    private void evictOne() {
        entries.keySet().stream().findFirst().ifPresent(tenantId -> {
            entries.remove(tenantId);
            evicted.increment();
        });
    }

    private Counter counter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("koda.capabilities.cache.requests")
            .tag("result", result)
            .register(meterRegistry);
    }

    private record CacheEntry(TenantCapabilities capabilities, Instant expiresAt) {
    }
}

