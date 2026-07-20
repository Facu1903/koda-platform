package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record PurchaseProduct(
    UUID id,
    String sku,
    String name,
    String productType,
    String status,
    boolean stockTrackingEnabled,
    boolean allowNegativeStock
) {
}