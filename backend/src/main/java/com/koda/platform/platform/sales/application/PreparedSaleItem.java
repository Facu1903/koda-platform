package com.koda.platform.platform.sales.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PreparedSaleItem(
    int lineNumber,
    UUID productId,
    UUID warehouseId,
    String productSku,
    String productName,
    String productType,
    boolean stockTrackingEnabled,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal subtotalAmount
) {
}