package com.koda.platform.platform.stock.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateStockMovementCommand(
    UUID warehouseId,
    UUID productId,
    String movementType,
    BigDecimal quantity,
    BigDecimal unitCost,
    String currencyCode,
    String referenceType,
    UUID referenceId,
    String reason
) {
}