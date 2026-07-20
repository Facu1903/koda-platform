package com.koda.platform.platform.purchases.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemCommand(UUID productId, UUID warehouseId, BigDecimal quantity, BigDecimal unitCost) {
}