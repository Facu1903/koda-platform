package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class TenantCapabilitiesService {

    private final TenantCapabilitiesResolver resolver;
    private final CurrentTenantProvider currentTenantProvider;

    public TenantCapabilitiesService(TenantCapabilitiesResolver resolver, CurrentTenantProvider currentTenantProvider) {
        this.resolver = resolver;
        this.currentTenantProvider = currentTenantProvider;
    }

    public TenantCapabilities currentTenantCapabilities() {
        TenantContext context = currentTenantProvider.requireContext();
        return resolver.resolve(context.tenantId());
    }
}