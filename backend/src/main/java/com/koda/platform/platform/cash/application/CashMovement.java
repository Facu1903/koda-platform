package com.koda.platform.platform.cash.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashMovement(
    UUID id,
    TenantId tenantId,
    UUID cashSessionId,
    UUID cashRegisterId,
    UUID branchId,
    String movementType,
    String paymentMethod,
    BigDecimal amount,
    BigDecimal cashEffect,
    String currencyCode,
    String referenceType,
    UUID referenceId,
    String description,
    UUID createdByUserId,
    Instant occurredAt
) {
}