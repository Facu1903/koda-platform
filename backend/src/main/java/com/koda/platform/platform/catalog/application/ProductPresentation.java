package com.koda.platform.platform.catalog.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductPresentation(
    UUID id,
    TenantId tenantId,
    UUID unitId,
    String code,
    String name,
    BigDecimal quantity,
    boolean active,
    long version,
    Instant updatedAt
) {
}