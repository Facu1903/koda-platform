package com.koda.platform.platform.purchases.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Purchase(
    UUID id,
    TenantId tenantId,
    UUID branchId,
    UUID supplierId,
    long purchaseNumber,
    String numberCode,
    String supplierDocumentNumber,
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
    List<PurchaseItem> items
) {
}