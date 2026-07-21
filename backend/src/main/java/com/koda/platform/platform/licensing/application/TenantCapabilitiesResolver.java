package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantCapabilitiesResolver {

    private final TenantCapabilitiesRepository repository;
    private final TenantCapabilitiesCache cache;
    private final Clock clock;
    private final Duration cacheTtl;

    @Autowired
    public TenantCapabilitiesResolver(
        TenantCapabilitiesRepository repository,
        TenantCapabilitiesCache cache,
        @Value("${koda.licensing.capabilities-cache.ttl:30s}") Duration cacheTtl
    ) {
        this(repository, cache, Clock.systemUTC(), cacheTtl);
    }

    TenantCapabilitiesResolver(TenantCapabilitiesRepository repository, TenantCapabilitiesCache cache, Clock clock, Duration cacheTtl) {
        this.repository = repository;
        this.cache = cache;
        this.clock = clock;
        this.cacheTtl = cacheTtl == null ? Duration.ZERO : cacheTtl;
    }

    @Transactional(readOnly = true)
    public TenantCapabilities resolve(TenantId tenantId) {
        Instant calculatedAt = Instant.now(clock);
        return cache.find(tenantId, calculatedAt)
            .orElseGet(() -> calculateAndCache(tenantId, calculatedAt));
    }

    private TenantCapabilities calculateAndCache(TenantId tenantId, Instant calculatedAt) {
        TenantCapabilities capabilities = calculate(tenantId, calculatedAt);
        cache.put(capabilities, calculatedAt, cacheExpiresAt(capabilities, calculatedAt));
        return capabilities;
    }

    private TenantCapabilities calculate(TenantId tenantId, Instant calculatedAt) {
        TenantCapabilityTenant tenant = repository.findTenant(tenantId)
            .orElseThrow(() -> new TenantCapabilitiesUnavailableException("TENANT_NOT_FOUND", "Tenant capabilities cannot be resolved"));
        if (!tenant.active()) {
            throw new TenantCapabilitiesUnavailableException("TENANT_NOT_ACTIVE", "Tenant is not active");
        }

        List<ModuleCapability> modules = repository.findEnabledModules(tenantId, calculatedAt);
        Map<String, List<ModuleCapability>> modulesByProductCode = modules.stream()
            .collect(Collectors.groupingBy(ModuleCapability::productCode));
        List<ProductCapability> products = repository.findEnabledProducts(tenantId, calculatedAt).stream()
            .map(product -> product.withModules(modulesByProductCode.getOrDefault(product.code(), List.of()).stream()
                .sorted(Comparator.comparing(ModuleCapability::code))
                .toList()))
            .toList();

        return new TenantCapabilities(
            tenantId,
            tenant.active(),
            calculatedAt,
            products,
            repository.findEffectiveFeatureFlags(tenantId, calculatedAt),
            repository.findEffectiveLimits(tenantId, calculatedAt)
        );
    }

    private Instant cacheExpiresAt(TenantCapabilities capabilities, Instant now) {
        Instant ttlExpiresAt = now.plus(effectiveCacheTtl());
        return validityEnds(capabilities)
            .filter(Objects::nonNull)
            .filter(validUntil -> validUntil.isAfter(now))
            .min(Comparator.naturalOrder())
            .filter(validUntil -> validUntil.isBefore(ttlExpiresAt))
            .orElse(ttlExpiresAt);
    }

    private Duration effectiveCacheTtl() {
        return cacheTtl.isNegative() ? Duration.ZERO : cacheTtl;
    }

    private Stream<Instant> validityEnds(TenantCapabilities capabilities) {
        Stream<Instant> productValidityEnds = capabilities.products().stream()
            .flatMap(product -> Stream.concat(
                Stream.of(product.entitlementValidUntil(), product.subscriptionValidUntil()),
                product.modules().stream().map(ModuleCapability::validUntil)
            ));
        Stream<Instant> featureFlagValidityEnds = capabilities.featureFlags().stream()
            .map(FeatureFlagCapability::validUntil);
        Stream<Instant> limitValidityEnds = capabilities.limits().stream()
            .map(LimitCapability::validUntil);
        return Stream.of(productValidityEnds, featureFlagValidityEnds, limitValidityEnds).flatMap(stream -> stream);
    }
}

