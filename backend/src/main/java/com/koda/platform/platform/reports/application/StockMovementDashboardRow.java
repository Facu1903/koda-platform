package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementDashboardRow(
    UUID id,
    UUID warehouseId,
    String warehouseCode,
    String warehouseName,
    UUID productId,
    String productSku,
    String productName,
    String movementType,
    BigDecimal quantity,
    String referenceType,
    UUID referenceId,
    Instant confirmedAt
) {
}