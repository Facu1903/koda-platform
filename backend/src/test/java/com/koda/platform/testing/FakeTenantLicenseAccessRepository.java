package com.koda.platform.testing;

import com.koda.platform.platform.licensing.application.TenantLicenseAccessRepository;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class FakeTenantLicenseAccessRepository implements TenantLicenseAccessRepository {

    private boolean productEnabled = true;
    private boolean allModulesEnabled = true;
    private final Set<String> disabledModules = new HashSet<>();

    public void disableProduct() {
        productEnabled = false;
    }

    public void disableModule() {
        allModulesEnabled = false;
    }

    public void disableModule(String moduleCode) {
        disabledModules.add(moduleCode);
    }

    @Override
    public boolean isProductEnabled(TenantId tenantId, String productCode, Instant calculatedAt) {
        return productEnabled;
    }

    @Override
    public boolean isModuleEnabled(TenantId tenantId, String productCode, String moduleCode, Instant calculatedAt) {
        return allModulesEnabled && !disabledModules.contains(moduleCode);
    }
}