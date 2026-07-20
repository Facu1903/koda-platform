package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductSoldReportRow(
    UUID productId,
    String productSku,
    String productName,
    BigDecimal quantitySold,
    String currencyCode,
    BigDecimal totalAmount,
    long salesCount
) {
}