package com.koda.platform.platform.sales.application;

import com.koda.platform.shared.application.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;

public interface SalesCashPort {

    SalesCashMovement recordSalePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount, String currencyCode,
                                        UUID saleId, SalesRequestMetadata metadata);

    SalesCashMovement reverseSalePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount, String currencyCode,
                                         UUID saleId, SalesRequestMetadata metadata);
}