package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.UUID;

public record TenantProductEntitlementAdministration(
    UUID id,
    UUID productId,
    String productCode,
    String productName,
    String status,
    Instant validFrom,
    Instant validUntil,
    long version,
    Instant updatedAt
) {
}
