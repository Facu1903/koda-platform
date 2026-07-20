package com.koda.platform.platform.cash.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record CashRegister(
    UUID id,
    TenantId tenantId,
    UUID branchId,
    String code,
    String name,
    String status,
    long version,
    Instant updatedAt
) {
}