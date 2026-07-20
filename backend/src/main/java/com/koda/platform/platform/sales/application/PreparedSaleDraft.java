package com.koda.platform.platform.sales.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PreparedSaleDraft(
    UUID branchId,
    UUID customerId,
    String currencyCode,
    BigDecimal subtotalAmount,
    BigDecimal totalAmount,
    List<PreparedSaleItem> items
) {
}