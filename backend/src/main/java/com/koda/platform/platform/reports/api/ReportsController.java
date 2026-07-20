package com.koda.platform.platform.reports.api;

import com.koda.platform.platform.reports.application.CashMovementReportRow;
import com.koda.platform.platform.reports.application.CashMovementReportSummary;
import com.koda.platform.platform.reports.application.CashMovementsRangeReport;
import com.koda.platform.platform.reports.application.CashSessionDashboard;
import com.koda.platform.platform.reports.application.CommercialDocumentReportRow;
import com.koda.platform.platform.reports.application.CommercialReportSummary;
import com.koda.platform.platform.reports.application.LowStockReport;
import com.koda.platform.platform.reports.application.LowStockReportRow;
import com.koda.platform.platform.reports.application.OperationalDashboard;
import com.koda.platform.platform.reports.application.PurchasesRangeReport;
import com.koda.platform.platform.reports.application.ReportPeriod;
import com.koda.platform.platform.reports.application.ReportsService;
import com.koda.platform.platform.reports.application.SalesRangeReport;
import com.koda.platform.platform.reports.application.StockMovementDashboardRow;
import com.koda.platform.platform.reports.application.TopProductSoldReportRow;
import com.koda.platform.platform.reports.application.TopProductsSoldReport;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/sales")
    public SalesRangeReportResponse salesByRange(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return SalesRangeReportResponse.from(reportsService.salesByRange(from, to, limit));
    }

    @GetMapping("/purchases")
    public PurchasesRangeReportResponse purchasesByRange(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return PurchasesRangeReportResponse.from(reportsService.purchasesByRange(from, to, limit));
    }

    @GetMapping("/cash-movements")
    public CashMovementsRangeReportResponse cashMovementsByRange(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return CashMovementsRangeReportResponse.from(reportsService.cashMovementsByRange(from, to, limit));
    }

    @GetMapping("/top-products-sold")
    public TopProductsSoldReportResponse topProductsSold(
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(defaultValue = "10") @Min(1) @Max(500) int limit
    ) {
        return TopProductsSoldReportResponse.from(reportsService.topProductsSold(from, to, limit));
    }

    @GetMapping("/low-stock")
    public LowStockReportResponse lowStock(
        @RequestParam(defaultValue = "0") @DecimalMin(value = "0.000000", inclusive = true) BigDecimal threshold,
        @RequestParam(defaultValue = "50") @Min(1) @Max(500) int limit
    ) {
        return LowStockReportResponse.from(reportsService.lowStock(threshold, limit));
    }

    @GetMapping("/dashboard")
    public OperationalDashboardResponse dashboard(
        @RequestParam(defaultValue = "0") @DecimalMin(value = "0.000000", inclusive = true) BigDecimal lowStockThreshold,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int lowStockLimit,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int stockMovementLimit
    ) {
        return OperationalDashboardResponse.from(reportsService.dashboard(lowStockThreshold, lowStockLimit, stockMovementLimit));
    }

    public record ReportPeriodResponse(Instant from, Instant to) {
        static ReportPeriodResponse from(ReportPeriod period) {
            return new ReportPeriodResponse(period.from(), period.to());
        }
    }

    public record CommercialSummaryResponse(
        String currencyCode,
        long confirmedCount,
        BigDecimal confirmedTotalAmount,
        long cancelledCount,
        BigDecimal cancelledTotalAmount,
        long paidCount,
        BigDecimal paidTotalAmount
    ) {
        static CommercialSummaryResponse from(CommercialReportSummary summary) {
            return new CommercialSummaryResponse(summary.currencyCode(), summary.confirmedCount(), summary.confirmedTotalAmount(),
                summary.cancelledCount(), summary.cancelledTotalAmount(), summary.paidCount(), summary.paidTotalAmount());
        }
    }

    public record CommercialDocumentRowResponse(
        UUID id,
        UUID branchId,
        UUID partnerId,
        String partnerName,
        long documentNumber,
        String numberCode,
        String status,
        String currencyCode,
        BigDecimal totalAmount,
        String paymentStatus,
        BigDecimal paidAmount,
        Instant confirmedAt,
        Instant cancelledAt
    ) {
        static CommercialDocumentRowResponse from(CommercialDocumentReportRow row) {
            return new CommercialDocumentRowResponse(row.id(), row.branchId(), row.partnerId(), row.partnerName(), row.documentNumber(), row.numberCode(),
                row.status(), row.currencyCode(), row.totalAmount(), row.paymentStatus(), row.paidAmount(), row.confirmedAt(), row.cancelledAt());
        }
    }

    public record SalesRangeReportResponse(
        ReportPeriodResponse period,
        CommercialSummaryResponse summary,
        List<CommercialDocumentRowResponse> rows
    ) {
        static SalesRangeReportResponse from(SalesRangeReport report) {
            return new SalesRangeReportResponse(ReportPeriodResponse.from(report.period()), CommercialSummaryResponse.from(report.summary()),
                report.rows().stream().map(CommercialDocumentRowResponse::from).toList());
        }
    }

    public record PurchasesRangeReportResponse(
        ReportPeriodResponse period,
        CommercialSummaryResponse summary,
        List<CommercialDocumentRowResponse> rows
    ) {
        static PurchasesRangeReportResponse from(PurchasesRangeReport report) {
            return new PurchasesRangeReportResponse(ReportPeriodResponse.from(report.period()), CommercialSummaryResponse.from(report.summary()),
                report.rows().stream().map(CommercialDocumentRowResponse::from).toList());
        }
    }

    public record CashMovementSummaryResponse(
        String currencyCode,
        long movementCount,
        BigDecimal totalCashIn,
        BigDecimal totalCashOut,
        BigDecimal netCashEffect
    ) {
        static CashMovementSummaryResponse from(CashMovementReportSummary summary) {
            return new CashMovementSummaryResponse(summary.currencyCode(), summary.movementCount(), summary.totalCashIn(), summary.totalCashOut(),
                summary.netCashEffect());
        }
    }

    public record CashMovementRowResponse(
        UUID id,
        UUID cashSessionId,
        UUID cashRegisterId,
        UUID branchId,
        String movementType,
        String paymentMethod,
        BigDecimal amount,
        BigDecimal cashEffect,
        String currencyCode,
        String referenceType,
        UUID referenceId,
        UUID createdByUserId,
        Instant occurredAt
    ) {
        static CashMovementRowResponse from(CashMovementReportRow row) {
            return new CashMovementRowResponse(row.id(), row.cashSessionId(), row.cashRegisterId(), row.branchId(), row.movementType(),
                row.paymentMethod(), row.amount(), row.cashEffect(), row.currencyCode(), row.referenceType(), row.referenceId(), row.createdByUserId(),
                row.occurredAt());
        }
    }

    public record CashMovementsRangeReportResponse(
        ReportPeriodResponse period,
        CashMovementSummaryResponse summary,
        List<CashMovementRowResponse> rows
    ) {
        static CashMovementsRangeReportResponse from(CashMovementsRangeReport report) {
            return new CashMovementsRangeReportResponse(ReportPeriodResponse.from(report.period()), CashMovementSummaryResponse.from(report.summary()),
                report.rows().stream().map(CashMovementRowResponse::from).toList());
        }
    }

    public record TopProductSoldRowResponse(
        UUID productId,
        String productSku,
        String productName,
        BigDecimal quantitySold,
        String currencyCode,
        BigDecimal totalAmount,
        long salesCount
    ) {
        static TopProductSoldRowResponse from(TopProductSoldReportRow row) {
            return new TopProductSoldRowResponse(row.productId(), row.productSku(), row.productName(), row.quantitySold(), row.currencyCode(),
                row.totalAmount(), row.salesCount());
        }
    }

    public record TopProductsSoldReportResponse(ReportPeriodResponse period, List<TopProductSoldRowResponse> rows) {
        static TopProductsSoldReportResponse from(TopProductsSoldReport report) {
            return new TopProductsSoldReportResponse(ReportPeriodResponse.from(report.period()),
                report.rows().stream().map(TopProductSoldRowResponse::from).toList());
        }
    }

    public record LowStockRowResponse(
        UUID stockBalanceId,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        UUID productId,
        String productSku,
        String productName,
        BigDecimal quantityOnHand,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        Instant updatedAt
    ) {
        static LowStockRowResponse from(LowStockReportRow row) {
            return new LowStockRowResponse(row.stockBalanceId(), row.warehouseId(), row.warehouseCode(), row.warehouseName(), row.productId(),
                row.productSku(), row.productName(), row.quantityOnHand(), row.reservedQuantity(), row.availableQuantity(), row.updatedAt());
        }
    }

    public record LowStockReportResponse(BigDecimal threshold, List<LowStockRowResponse> rows) {
        static LowStockReportResponse from(LowStockReport report) {
            return new LowStockReportResponse(report.threshold(), report.rows().stream().map(LowStockRowResponse::from).toList());
        }
    }

    public record CashSessionDashboardResponse(
        UUID id,
        UUID cashRegisterId,
        String cashRegisterCode,
        String cashRegisterName,
        UUID branchId,
        String status,
        BigDecimal openingAmount,
        BigDecimal expectedClosingAmount,
        String currencyCode,
        Instant openedAt,
        Instant updatedAt
    ) {
        static CashSessionDashboardResponse from(CashSessionDashboard session) {
            if (session == null) {
                return null;
            }
            return new CashSessionDashboardResponse(session.id(), session.cashRegisterId(), session.cashRegisterCode(), session.cashRegisterName(),
                session.branchId(), session.status(), session.openingAmount(), session.expectedClosingAmount(), session.currencyCode(), session.openedAt(),
                session.updatedAt());
        }
    }

    public record StockMovementDashboardRowResponse(
        UUID id,
        UUID warehouseId,
        String warehouseCode,
        String warehouseName,
        UUID productId,
        String productSku,
        String productName,
        String movementType,
        BigDecimal quantity,
        String referenceType,
        UUID referenceId,
        Instant confirmedAt
    ) {
        static StockMovementDashboardRowResponse from(StockMovementDashboardRow row) {
            return new StockMovementDashboardRowResponse(row.id(), row.warehouseId(), row.warehouseCode(), row.warehouseName(), row.productId(),
                row.productSku(), row.productName(), row.movementType(), row.quantity(), row.referenceType(), row.referenceId(), row.confirmedAt());
        }
    }

    public record OperationalDashboardResponse(
        LocalDate businessDate,
        ReportPeriodResponse period,
        String currencyCode,
        long salesCount,
        BigDecimal salesTotalAmount,
        long purchasesCount,
        BigDecimal purchasesTotalAmount,
        CashSessionDashboardResponse currentCashSession,
        List<LowStockRowResponse> lowStockProducts,
        List<StockMovementDashboardRowResponse> latestStockMovements
    ) {
        static OperationalDashboardResponse from(OperationalDashboard dashboard) {
            return new OperationalDashboardResponse(dashboard.businessDate(), ReportPeriodResponse.from(dashboard.period()), dashboard.currencyCode(),
                dashboard.salesCount(), dashboard.salesTotalAmount(), dashboard.purchasesCount(), dashboard.purchasesTotalAmount(),
                CashSessionDashboardResponse.from(dashboard.currentCashSession()),
                dashboard.lowStockProducts().stream().map(LowStockRowResponse::from).toList(),
                dashboard.latestStockMovements().stream().map(StockMovementDashboardRowResponse::from).toList());
        }
    }
}