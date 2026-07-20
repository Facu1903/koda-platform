package com.koda.platform.platform.sales.application;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemCommand(UUID productId, UUID warehouseId, BigDecimal quantity, BigDecimal unitPrice) {
}