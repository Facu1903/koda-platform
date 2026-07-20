package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;

public record CommercialReportSummary(
    String currencyCode,
    long confirmedCount,
    BigDecimal confirmedTotalAmount,
    long cancelledCount,
    BigDecimal cancelledTotalAmount,
    long paidCount,
    BigDecimal paidTotalAmount
) {
}