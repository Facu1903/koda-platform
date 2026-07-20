package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LowStockReportRow(
    UUID stockBalanceId,
    UUID warehouseId,
    String warehouseCode,
    String warehouseName,
    UUID productId,
    String productSku,
    String productName,
    BigDecimal quantityOnHand,
    BigDecimal reservedQuantity,
    BigDecimal availableQuantity,
    Instant updatedAt
) {
}