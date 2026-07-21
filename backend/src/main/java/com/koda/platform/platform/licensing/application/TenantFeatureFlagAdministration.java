package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.UUID;

public record TenantFeatureFlagAdministration(
    UUID id,
    UUID productId,
    String productCode,
    String productName,
    UUID moduleId,
    String moduleCode,
    String code,
    boolean enabled,
    String reason,
    Instant validFrom,
    Instant validUntil,
    long version,
    Instant updatedAt
) {
}
