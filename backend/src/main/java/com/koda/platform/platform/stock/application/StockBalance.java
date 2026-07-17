package com.koda.platform.platform.stock.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockBalance(
    UUID id,
    TenantId tenantId,
    UUID warehouseId,
    UUID productId,
    BigDecimal quantityOnHand,
    BigDecimal reservedQuantity,
    long version,
    Instant updatedAt
) {
}