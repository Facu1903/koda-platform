package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record SaleItemStockReversal(UUID saleItemId, UUID stockReversalMovementId) {
}