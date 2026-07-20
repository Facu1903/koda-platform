package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantCapabilitiesService {

    private final TenantCapabilitiesRepository repository;
    private final CurrentTenantProvider currentTenantProvider;
    private final Clock clock;

    @Autowired
    public TenantCapabilitiesService(TenantCapabilitiesRepository repository, CurrentTenantProvider currentTenantProvider) {
        this(repository, currentTenantProvider, Clock.systemUTC());
    }

    TenantCapabilitiesService(TenantCapabilitiesRepository repository, CurrentTenantProvider currentTenantProvider, Clock clock) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TenantCapabilities currentTenantCapabilities() {
        TenantContext context = currentTenantProvider.requireContext();
        Instant calculatedAt = Instant.now(clock);
        TenantCapabilityTenant tenant = repository.findTenant(context.tenantId())
            .orElseThrow(() -> new TenantCapabilitiesUnavailableException("TENANT_NOT_FOUND", "Tenant capabilities cannot be resolved"));
        if (!tenant.active()) {
            throw new TenantCapabilitiesUnavailableException("TENANT_NOT_ACTIVE", "Tenant is not active");
        }

        List<ModuleCapability> modules = repository.findEnabledModules(context.tenantId(), calculatedAt);
        Map<String, List<ModuleCapability>> modulesByProductCode = modules.stream()
            .collect(Collectors.groupingBy(ModuleCapability::productCode));
        List<ProductCapability> products = repository.findEnabledProducts(context.tenantId(), calculatedAt).stream()
            .map(product -> product.withModules(modulesByProductCode.getOrDefault(product.code(), List.of()).stream()
                .sorted(Comparator.comparing(ModuleCapability::code))
                .toList()))
            .toList();

        return new TenantCapabilities(
            context.tenantId(),
            tenant.active(),
            calculatedAt,
            products,
            repository.findEffectiveFeatureFlags(context.tenantId(), calculatedAt),
            repository.findEffectiveLimits(context.tenantId(), calculatedAt)
        );
    }
}