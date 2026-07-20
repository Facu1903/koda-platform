package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record PurchaseItemStockUpdate(UUID purchaseItemId, UUID stockMovementId) {
}