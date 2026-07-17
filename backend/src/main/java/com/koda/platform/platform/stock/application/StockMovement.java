package com.koda.platform.platform.stock.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovement(
    UUID id,
    TenantId tenantId,
    UUID warehouseId,
    UUID productId,
    String movementType,
    BigDecimal quantity,
    BigDecimal quantityBefore,
    BigDecimal quantityAfter,
    BigDecimal quantityDelta,
    BigDecimal unitCost,
    String currencyCode,
    String referenceType,
    UUID referenceId,
    UUID reversalOfMovementId,
    String reason,
    Instant confirmedAt,
    UUID confirmedBy
) {
}