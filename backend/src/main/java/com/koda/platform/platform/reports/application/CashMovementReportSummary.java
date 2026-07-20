package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;

public record CashMovementReportSummary(
    String currencyCode,
    long movementCount,
    BigDecimal totalCashIn,
    BigDecimal totalCashOut,
    BigDecimal netCashEffect
) {
}