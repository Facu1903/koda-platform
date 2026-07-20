package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;

public interface TenantLicenseAccessRepository {

    boolean isProductEnabled(TenantId tenantId, String productCode, Instant calculatedAt);

    boolean isModuleEnabled(TenantId tenantId, String productCode, String moduleCode, Instant calculatedAt);
}
