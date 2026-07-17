package com.koda.platform.platform.catalog.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record Product(
    UUID id,
    TenantId tenantId,
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
    boolean stockTrackingEnabled,
    boolean allowNegativeStock,
    long version,
    Instant updatedAt
) {
}