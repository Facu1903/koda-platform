package com.koda.platform.platform.reports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.platform.licensing.application.TenantLicenseAccessDeniedException;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessGuard;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.testing.FakeTenantLicenseAccessRepository;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportsServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private FakeCurrentTenantProvider currentTenantProvider;
    private FakeTenantLicenseAccessRepository licenseAccessRepository;
    private FakeReportsRepository repository;
    private ReportsService service;

    @BeforeEach
    void setUp() {
        currentTenantProvider = new FakeCurrentTenantProvider();
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("MANAGER"), Set.of("commercial_reports:read"), false));
        repository = new FakeReportsRepository();
        licenseAccessRepository = new FakeTenantLicenseAccessRepository();
        service = new ReportsService(repository, currentTenantProvider, new TenantLicenseAccessGuard(licenseAccessRepository),
            Clock.fixed(Instant.parse("2026-07-20T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void salesReportRequiresCommercialReportsPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of("READ_ONLY"), Set.of(), false));

        assertThatThrownBy(() -> service.salesByRange(Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"), 50))
            .isInstanceOf(PermissionDeniedException.class)
            .satisfies(exception -> assertThat(((PermissionDeniedException) exception).requiredPermission()).isEqualTo("commercial_reports:read"));
    }

    @Test
    void commercialReportsModuleDisabledBlocksReportsEvenWithPermission() {
        licenseAccessRepository.disableModule();

        assertThatThrownBy(() -> service.salesByRange(Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"), 50))
            .isInstanceOf(TenantLicenseAccessDeniedException.class)
            .satisfies(exception -> assertThat(((TenantLicenseAccessDeniedException) exception).reasonCode()).isEqualTo("MODULE_NOT_ENABLED"));
    }

    @Test
    void salesReportNormalizesRangeAndCapsLimit() {
        SalesRangeReport report = service.salesByRange(Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"), 700);

        assertThat(report.summary().currencyCode()).isEqualTo("ARS");
        assertThat(repository.lastRange.limit()).isEqualTo(500);
        assertThat(repository.salesRangeCalls).isEqualTo(1);
    }

    @Test
    void reportsRequireValidRange() {
        assertThatThrownBy(() -> service.purchasesByRange(null, Instant.parse("2026-07-02T00:00:00Z"), 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Report from date is required");
        assertThatThrownBy(() -> service.purchasesByRange(Instant.parse("2026-07-02T00:00:00Z"), Instant.parse("2026-07-02T00:00:00Z"), 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Report from date must be before to date");
        assertThatThrownBy(() -> service.purchasesByRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-02-01T00:00:00Z"), 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Report range cannot exceed 366 days");
    }

    @Test
    void lowStockUsesDefaultThresholdAndCapsLimit() {
        LowStockReport report = service.lowStock(null, 900);

        assertThat(report.threshold()).isEqualByComparingTo("0.000000");
        assertThat(repository.lastLowStockThreshold).isEqualByComparingTo("0.000000");
        assertThat(repository.lastLowStockLimit).isEqualTo(500);
    }

    @Test
    void lowStockRejectsNegativeThreshold() {
        assertThatThrownBy(() -> service.lowStock(new BigDecimal("-0.000001"), 50))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Low stock threshold cannot be negative");
    }

    @Test
    void dashboardUsesTenantTimeZoneForBusinessDate() {
        OperationalDashboard dashboard = service.dashboard(new BigDecimal("2"), 20, 30);

        assertThat(dashboard.businessDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(dashboard.period().from()).isEqualTo(Instant.parse("2026-07-20T03:00:00Z"));
        assertThat(dashboard.period().to()).isEqualTo(Instant.parse("2026-07-21T03:00:00Z"));
        assertThat(dashboard.salesCount()).isEqualTo(3);
        assertThat(dashboard.salesTotalAmount()).isEqualByComparingTo("1500.0000");
        assertThat(dashboard.purchasesCount()).isEqualTo(2);
        assertThat(dashboard.purchasesTotalAmount()).isEqualByComparingTo("900.0000");
        assertThat(repository.lastLowStockLimit).isEqualTo(20);
        assertThat(repository.lastStockMovementLimit).isEqualTo(30);
    }

    @Test
    void dashboardCapsSmallLists() {
        service.dashboard(BigDecimal.ZERO, 500, 500);

        assertThat(repository.lastLowStockLimit).isEqualTo(100);
        assertThat(repository.lastStockMovementLimit).isEqualTo(100);
    }

    private static class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private class FakeReportsRepository implements ReportsRepository {
        private ReportRange lastRange;
        private BigDecimal lastLowStockThreshold;
        private int lastLowStockLimit;
        private int lastStockMovementLimit;
        private int salesRangeCalls;

        @Override
        public Optional<TenantReportSettings> findTenantReportSettings(TenantId tenantId) {
            return Optional.of(new TenantReportSettings("ARS", "America/Argentina/Buenos_Aires"));
        }

        @Override
        public SalesRangeReport salesByRange(TenantId tenantId, ReportRange range, String currencyCode) {
            lastRange = range;
            salesRangeCalls++;
            return new SalesRangeReport(new ReportPeriod(range.from(), range.to()), emptyCommercialSummary(currencyCode), List.of());
        }

        @Override
        public PurchasesRangeReport purchasesByRange(TenantId tenantId, ReportRange range, String currencyCode) {
            lastRange = range;
            return new PurchasesRangeReport(new ReportPeriod(range.from(), range.to()), emptyCommercialSummary(currencyCode), List.of());
        }

        @Override
        public CashMovementsRangeReport cashMovementsByRange(TenantId tenantId, ReportRange range, String currencyCode) {
            lastRange = range;
            return new CashMovementsRangeReport(new ReportPeriod(range.from(), range.to()),
                new CashMovementReportSummary(currencyCode, 0, money("0"), money("0"), money("0")), List.of());
        }

        @Override
        public TopProductsSoldReport topProductsSold(TenantId tenantId, ReportRange range, String currencyCode) {
            lastRange = range;
            return new TopProductsSoldReport(new ReportPeriod(range.from(), range.to()), List.of());
        }

        @Override
        public LowStockReport lowStock(TenantId tenantId, BigDecimal threshold, int limit) {
            lastLowStockThreshold = threshold;
            lastLowStockLimit = limit;
            return new LowStockReport(threshold, List.of());
        }

        @Override
        public CommercialReportSummary summarizeSales(TenantId tenantId, Instant from, Instant to, String currencyCode) {
            return new CommercialReportSummary(currencyCode, 3, money("1500"), 1, money("500"), 2, money("1000"));
        }

        @Override
        public CommercialReportSummary summarizePurchases(TenantId tenantId, Instant from, Instant to, String currencyCode) {
            return new CommercialReportSummary(currencyCode, 2, money("900"), 0, money("0"), 1, money("700"));
        }

        @Override
        public Optional<CashSessionDashboard> findCurrentCashSession(TenantId tenantId, UUID userId) {
            return Optional.empty();
        }

        @Override
        public List<StockMovementDashboardRow> latestStockMovements(TenantId tenantId, int limit) {
            lastStockMovementLimit = limit;
            return List.of();
        }

        private CommercialReportSummary emptyCommercialSummary(String currencyCode) {
            return new CommercialReportSummary(currencyCode, 0, money("0"), 0, money("0"), 0, money("0"));
        }

        private BigDecimal money(String value) {
            return new BigDecimal(value).setScale(4);
        }
    }
}