package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record SalesProduct(
    UUID id,
    String sku,
    String name,
    String productType,
    String status,
    boolean stockTrackingEnabled,
    boolean allowNegativeStock
) {
}