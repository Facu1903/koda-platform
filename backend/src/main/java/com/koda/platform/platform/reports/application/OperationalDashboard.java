package com.koda.platform.platform.reports.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OperationalDashboard(
    LocalDate businessDate,
    ReportPeriod period,
    String currencyCode,
    long salesCount,
    BigDecimal salesTotalAmount,
    long purchasesCount,
    BigDecimal purchasesTotalAmount,
    CashSessionDashboard currentCashSession,
    List<LowStockReportRow> lowStockProducts,
    List<StockMovementDashboardRow> latestStockMovements
) {
}