package com.koda.platform.platform.sales.application;

import com.koda.platform.shared.application.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;

public interface SalesStockPort {

    SalesStockMovement issueSaleStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleId, UUID saleItemId,
                                      SalesRequestMetadata metadata);

    SalesStockMovement reverseSaleStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleId, UUID saleItemId,
                                        SalesRequestMetadata metadata);
}