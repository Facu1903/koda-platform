package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.UUID;

public record TenantLimitOverrideAdministration(
    UUID id,
    UUID productId,
    String productCode,
    String productName,
    String code,
    Long value,
    boolean unlimited,
    String unit,
    String reason,
    Instant validFrom,
    Instant validUntil,
    long version,
    Instant updatedAt
) {
}
