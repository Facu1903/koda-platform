package com.koda.platform.platform.catalog.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record Brand(
    UUID id,
    TenantId tenantId,
    String code,
    String name,
    String description,
    boolean active,
    long version,
    Instant updatedAt
) {
}