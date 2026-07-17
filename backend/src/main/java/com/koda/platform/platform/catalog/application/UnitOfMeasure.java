package com.koda.platform.platform.catalog.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record UnitOfMeasure(
    UUID id,
    TenantId tenantId,
    String code,
    String name,
    String symbol,
    int decimalPrecision,
    boolean active,
    long version,
    Instant updatedAt
) {
}