package com.koda.platform.platform.sales.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Sale(
    UUID id,
    TenantId tenantId,
    UUID branchId,
    UUID customerId,
    long saleNumber,
    String numberCode,
    String status,
    String currencyCode,
    BigDecimal subtotalAmount,
    BigDecimal totalAmount,
    String paymentStatus,
    String paymentMethod,
    BigDecimal paidAmount,
    UUID cashSessionId,
    UUID cashMovementId,
    UUID paymentReversalCashSessionId,
    UUID paymentReversalCashMovementId,
    Instant confirmedAt,
    UUID confirmedBy,
    Instant cancelledAt,
    UUID cancelledBy,
    String cancellationReason,
    long version,
    Instant updatedAt,
    List<SaleItem> items
) {
}