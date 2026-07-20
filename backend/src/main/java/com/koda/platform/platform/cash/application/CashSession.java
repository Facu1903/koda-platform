package com.koda.platform.platform.cash.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashSession(
    UUID id,
    TenantId tenantId,
    UUID cashRegisterId,
    UUID branchId,
    UUID openedByUserId,
    String status,
    BigDecimal openingAmount,
    String currencyCode,
    BigDecimal expectedClosingAmount,
    BigDecimal countedClosingAmount,
    BigDecimal closingDifference,
    Instant openedAt,
    Instant closedAt,
    long version,
    Instant updatedAt
) {
}