package com.koda.platform.platform.catalog.application;

import java.util.UUID;

public record CreateProductCommand(
    String sku,
    String name,
    String description,
    String barcode,
    UUID brandId,
    UUID categoryId,
    UUID baseUnitId,
    UUID defaultPresentationId,
    String productType,
    String status,
    Boolean stockTrackingEnabled
) {
}
