package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.UUID;

public record TenantProductSubscriptionAdministration(
    UUID id,
    UUID productId,
    String productCode,
    String productName,
    UUID planId,
    String planCode,
    String planName,
    String status,
    Instant validFrom,
    Instant validUntil,
    String source,
    Instant cancelledAt,
    long version,
    Instant updatedAt
) {
}
