package com.koda.platform.platform.purchases.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItem(
    UUID id,
    UUID purchaseId,
    int lineNumber,
    UUID productId,
    UUID warehouseId,
    String productSku,
    String productName,
    String productType,
    boolean stockTrackingEnabled,
    BigDecimal quantity,
    BigDecimal unitCost,
    BigDecimal subtotalAmount,
    UUID stockMovementId,
    UUID stockReversalMovementId
) {
}