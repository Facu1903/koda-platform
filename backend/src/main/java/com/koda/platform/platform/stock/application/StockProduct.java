package com.koda.platform.platform.stock.application;

import java.util.UUID;

public record StockProduct(
    UUID id,
    String productType,
    String status,
    boolean stockTrackingEnabled,
    boolean allowNegativeStock
) {
}