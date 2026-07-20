package com.koda.platform.platform.reports.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportsRepository {

    Optional<TenantReportSettings> findTenantReportSettings(TenantId tenantId);

    SalesRangeReport salesByRange(TenantId tenantId, ReportRange range, String currencyCode);

    PurchasesRangeReport purchasesByRange(TenantId tenantId, ReportRange range, String currencyCode);

    CashMovementsRangeReport cashMovementsByRange(TenantId tenantId, ReportRange range, String currencyCode);

    TopProductsSoldReport topProductsSold(TenantId tenantId, ReportRange range, String currencyCode);

    LowStockReport lowStock(TenantId tenantId, BigDecimal threshold, int limit);

    CommercialReportSummary summarizeSales(TenantId tenantId, Instant from, Instant to, String currencyCode);

    CommercialReportSummary summarizePurchases(TenantId tenantId, Instant from, Instant to, String currencyCode);

    Optional<CashSessionDashboard> findCurrentCashSession(TenantId tenantId, UUID userId);

    List<StockMovementDashboardRow> latestStockMovements(TenantId tenantId, int limit);
}