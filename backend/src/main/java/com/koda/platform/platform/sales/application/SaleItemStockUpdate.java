package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record SaleItemStockUpdate(UUID saleItemId, UUID stockMovementId) {
}