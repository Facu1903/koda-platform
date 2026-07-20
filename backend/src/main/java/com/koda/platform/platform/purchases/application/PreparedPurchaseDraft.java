package com.koda.platform.platform.purchases.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PreparedPurchaseDraft(
    UUID branchId,
    UUID supplierId,
    String supplierDocumentNumber,
    String currencyCode,
    BigDecimal subtotalAmount,
    BigDecimal totalAmount,
    List<PreparedPurchaseItem> items
) {
}