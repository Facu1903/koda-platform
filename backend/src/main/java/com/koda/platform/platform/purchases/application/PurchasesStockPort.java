package com.koda.platform.platform.purchases.application;

import com.koda.platform.shared.application.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;

public interface PurchasesStockPort {

    PurchasesStockMovement receivePurchaseStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID purchaseId,
                                                UUID purchaseItemId, PurchasesRequestMetadata metadata);

    PurchasesStockMovement reversePurchaseStock(TenantContext context, UUID warehouseId, UUID productId, BigDecimal quantity, UUID purchaseId,
                                                UUID purchaseItemId, PurchasesRequestMetadata metadata);
}