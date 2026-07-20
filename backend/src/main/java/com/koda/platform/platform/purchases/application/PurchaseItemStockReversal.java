package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record PurchaseItemStockReversal(UUID purchaseItemId, UUID stockReversalMovementId) {
}