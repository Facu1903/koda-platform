package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.UUID;

public record TenantModuleEntitlementAdministration(
    UUID id,
    UUID productId,
    String productCode,
    String productName,
    UUID moduleId,
    String moduleCode,
    String moduleName,
    boolean coreModule,
    boolean commerciallyToggleable,
    String status,
    Instant validFrom,
    Instant validUntil,
    long version,
    Instant updatedAt
) {
}
