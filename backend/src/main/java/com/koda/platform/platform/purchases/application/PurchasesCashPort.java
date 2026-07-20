package com.koda.platform.platform.purchases.application;

import com.koda.platform.shared.application.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;

public interface PurchasesCashPort {

    PurchasesCashMovement recordPurchasePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount, String currencyCode,
                                                UUID purchaseId, PurchasesRequestMetadata metadata);

    PurchasesCashMovement reversePurchasePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount, String currencyCode,
                                                 UUID purchaseId, PurchasesRequestMetadata metadata);
}