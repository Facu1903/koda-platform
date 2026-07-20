package com.koda.platform.platform.reports.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportsService {

    private static final String REPORTS_READ_PERMISSION = "commercial_reports:read";
    private static final int MAX_LIMIT = 500;
    private static final int MAX_DASHBOARD_LIMIT = 100;
    private static final long MAX_RANGE_DAYS = 366;
    private static final int QUANTITY_SCALE = 6;
    private static final BigDecimal DEFAULT_LOW_STOCK_THRESHOLD = BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);

    private final ReportsRepository repository;
    private final CurrentTenantProvider currentTenantProvider;
    private final Clock clock;

    @Autowired
    public ReportsService(ReportsRepository repository, CurrentTenantProvider currentTenantProvider) {
        this(repository, currentTenantProvider, Clock.systemUTC());
    }

    ReportsService(ReportsRepository repository, CurrentTenantProvider currentTenantProvider, Clock clock) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SalesRangeReport salesByRange(Instant from, Instant to, int limit) {
        TenantContext context = requireReportsPermission();
        TenantReportSettings settings = requireSettings(context.tenantId());
        ReportRange range = normalizeRange(from, to, limit);
        return repository.salesByRange(context.tenantId(), range, settings.defaultCurrency());
    }

    @Transactional(readOnly = true)
    public PurchasesRangeReport purchasesByRange(Instant from, Instant to, int limit) {
        TenantContext context = requireReportsPermission();
        TenantReportSettings settings = requireSettings(context.tenantId());
        ReportRange range = normalizeRange(from, to, limit);
        return repository.purchasesByRange(context.tenantId(), range, settings.defaultCurrency());
    }

    @Transactional(readOnly = true)
    public CashMovementsRangeReport cashMovementsByRange(Instant from, Instant to, int limit) {
        TenantContext context = requireReportsPermission();
        TenantReportSettings settings = requireSettings(context.tenantId());
        ReportRange range = normalizeRange(from, to, limit);
        return repository.cashMovementsByRange(context.tenantId(), range, settings.defaultCurrency());
    }

    @Transactional(readOnly = true)
    public TopProductsSoldReport topProductsSold(Instant from, Instant to, int limit) {
        TenantContext context = requireReportsPermission();
        TenantReportSettings settings = requireSettings(context.tenantId());
        ReportRange range = normalizeRange(from, to, limit);
        return repository.topProductsSold(context.tenantId(), range, settings.defaultCurrency());
    }

    @Transactional(readOnly = true)
    public LowStockReport lowStock(BigDecimal threshold, int limit) {
        TenantContext context = requireReportsPermission();
        return repository.lowStock(context.tenantId(), normalizeThreshold(threshold), normalizeLimit(limit, MAX_LIMIT));
    }

    @Transactional(readOnly = true)
    public OperationalDashboard dashboard(BigDecimal lowStockThreshold, int lowStockLimit, int stockMovementLimit) {
        TenantContext context = requireReportsPermission();
        TenantId tenantId = context.tenantId();
        TenantReportSettings settings = requireSettings(tenantId);
        ZoneId zoneId = zoneId(settings.timeZone());
        LocalDate businessDate = Instant.now(clock).atZone(zoneId).toLocalDate();
        ZonedDateTime startOfDay = businessDate.atStartOfDay(zoneId);
        Instant from = startOfDay.toInstant();
        Instant to = startOfDay.plusDays(1).toInstant();
        BigDecimal threshold = normalizeThreshold(lowStockThreshold);
        int normalizedLowStockLimit = normalizeLimit(lowStockLimit, MAX_DASHBOARD_LIMIT);
        int normalizedStockMovementLimit = normalizeLimit(stockMovementLimit, MAX_DASHBOARD_LIMIT);

        CommercialReportSummary sales = repository.summarizeSales(tenantId, from, to, settings.defaultCurrency());
        CommercialReportSummary purchases = repository.summarizePurchases(tenantId, from, to, settings.defaultCurrency());
        LowStockReport lowStock = repository.lowStock(tenantId, threshold, normalizedLowStockLimit);
        List<StockMovementDashboardRow> latestMovements = repository.latestStockMovements(tenantId, normalizedStockMovementLimit);

        return new OperationalDashboard(
            businessDate,
            new ReportPeriod(from, to),
            settings.defaultCurrency(),
            sales.confirmedCount(),
            sales.confirmedTotalAmount(),
            purchases.confirmedCount(),
            purchases.confirmedTotalAmount(),
            repository.findCurrentCashSession(tenantId, context.userId()).orElse(null),
            lowStock.rows(),
            latestMovements
        );
    }

    private TenantContext requireReportsPermission() {
        TenantContext context = currentTenantProvider.requireContext();
        if (context.platformAdmin() || context.hasPermission(REPORTS_READ_PERMISSION)) {
            return context;
        }
        throw new PermissionDeniedException(REPORTS_READ_PERMISSION);
    }

    private TenantReportSettings requireSettings(TenantId tenantId) {
        return repository.findTenantReportSettings(tenantId)
            .orElseThrow(() -> new IllegalStateException("Tenant report settings were not found"));
    }

    private ReportRange normalizeRange(Instant from, Instant to, int limit) {
        if (from == null) {
            throw new IllegalArgumentException("Report from date is required");
        }
        if (to == null) {
            throw new IllegalArgumentException("Report to date is required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Report from date must be before to date");
        }
        if (Duration.between(from, to).compareTo(Duration.ofDays(MAX_RANGE_DAYS)) > 0) {
            throw new IllegalArgumentException("Report range cannot exceed 366 days");
        }
        return new ReportRange(from, to, normalizeLimit(limit, MAX_LIMIT));
    }

    private int normalizeLimit(int limit, int max) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return Math.min(limit, max);
    }

    private BigDecimal normalizeThreshold(BigDecimal threshold) {
        if (threshold == null) {
            return DEFAULT_LOW_STOCK_THRESHOLD;
        }
        if (threshold.signum() < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative");
        }
        BigDecimal stripped = threshold.stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > QUANTITY_SCALE) {
            throw new IllegalArgumentException("Low stock threshold supports up to 6 decimals");
        }
        return threshold.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private ZoneId zoneId(String timeZone) {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw new IllegalStateException("Tenant time zone is invalid", exception);
        }
    }
}