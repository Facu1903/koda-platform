package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.application.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TenantLicenseAccessGuard {

    private final TenantLicenseAccessRepository repository;
    private final Clock clock;

    @Autowired
    public TenantLicenseAccessGuard(TenantLicenseAccessRepository repository) {
        this(repository, Clock.systemUTC());
    }

    TenantLicenseAccessGuard(TenantLicenseAccessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void requireProductEnabled(TenantContext context, String productCode) {
        Instant calculatedAt = Instant.now(clock);
        String normalizedProductCode = requireText(productCode, "Product code is required");
        if (repository.isProductEnabled(context.tenantId(), normalizedProductCode, calculatedAt)) {
            return;
        }
        throw new TenantLicenseAccessDeniedException(
            "PRODUCT_NOT_ENABLED",
            normalizedProductCode,
            null,
            "Product is not enabled for tenant"
        );
    }

    public void requireModuleEnabled(TenantContext context, String productCode, String moduleCode) {
        Instant calculatedAt = Instant.now(clock);
        String normalizedProductCode = requireText(productCode, "Product code is required");
        String normalizedModuleCode = requireText(moduleCode, "Module code is required");
        if (!repository.isProductEnabled(context.tenantId(), normalizedProductCode, calculatedAt)) {
            throw new TenantLicenseAccessDeniedException(
                "PRODUCT_NOT_ENABLED",
                normalizedProductCode,
                normalizedModuleCode,
                "Product is not enabled for tenant"
            );
        }
        if (repository.isModuleEnabled(context.tenantId(), normalizedProductCode, normalizedModuleCode, calculatedAt)) {
            return;
        }
        throw new TenantLicenseAccessDeniedException(
            "MODULE_NOT_ENABLED",
            normalizedProductCode,
            normalizedModuleCode,
            "Module is not enabled for tenant"
        );
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
