package com.koda.platform.platform.reports.infrastructure;

import com.koda.platform.platform.reports.application.CashMovementReportRow;
import com.koda.platform.platform.reports.application.CashMovementReportSummary;
import com.koda.platform.platform.reports.application.CashMovementsRangeReport;
import com.koda.platform.platform.reports.application.CashSessionDashboard;
import com.koda.platform.platform.reports.application.CommercialDocumentReportRow;
import com.koda.platform.platform.reports.application.CommercialReportSummary;
import com.koda.platform.platform.reports.application.LowStockReport;
import com.koda.platform.platform.reports.application.LowStockReportRow;
import com.koda.platform.platform.reports.application.PurchasesRangeReport;
import com.koda.platform.platform.reports.application.ReportPeriod;
import com.koda.platform.platform.reports.application.ReportRange;
import com.koda.platform.platform.reports.application.ReportsRepository;
import com.koda.platform.platform.reports.application.SalesRangeReport;
import com.koda.platform.platform.reports.application.StockMovementDashboardRow;
import com.koda.platform.platform.reports.application.TenantReportSettings;
import com.koda.platform.platform.reports.application.TopProductSoldReportRow;
import com.koda.platform.platform.reports.application.TopProductsSoldReport;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.reports.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcReportsRepository implements ReportsRepository {

    private static final int MONEY_SCALE = 4;
    private static final int QUANTITY_SCALE = 6;

    private final JdbcTemplate jdbcTemplate;

    public JdbcReportsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<TenantReportSettings> findTenantReportSettings(TenantId tenantId) {
        String sql = "SELECT default_currency, time_zone FROM tenants WHERE id = ? AND deleted_at IS NULL";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TenantReportSettings(rs.getString("default_currency"), rs.getString("time_zone")),
            tenantId.value()).stream().findFirst();
    }

    @Override
    public SalesRangeReport salesByRange(TenantId tenantId, ReportRange range, String currencyCode) {
        CommercialReportSummary summary = summarizeSales(tenantId, range.from(), range.to(), currencyCode);
        String sql = """
            SELECT s.id, s.branch_id, s.customer_id AS partner_id, bp.legal_name AS partner_name, s.sale_number AS document_number,
                   s.number_code, s.status, s.currency_code, s.total_amount, s.payment_status, s.paid_amount, s.confirmed_at, s.cancelled_at
            FROM sales_orders s
            JOIN business_partners bp ON bp.tenant_id = s.tenant_id AND bp.id = s.customer_id
            WHERE s.tenant_id = ?
              AND s.deleted_at IS NULL
              AND s.status IN ('CONFIRMED', 'CANCELLED')
              AND s.confirmed_at >= ?
              AND s.confirmed_at < ?
            ORDER BY s.confirmed_at DESC, s.sale_number DESC
            LIMIT ?
            """;
        List<CommercialDocumentReportRow> rows = jdbcTemplate.query(sql, this::mapCommercialDocumentRow, tenantId.value(), timestamp(range.from()),
            timestamp(range.to()), range.limit());
        return new SalesRangeReport(new ReportPeriod(range.from(), range.to()), summary, rows);
    }

    @Override
    public PurchasesRangeReport purchasesByRange(TenantId tenantId, ReportRange range, String currencyCode) {
        CommercialReportSummary summary = summarizePurchases(tenantId, range.from(), range.to(), currencyCode);
        String sql = """
            SELECT p.id, p.branch_id, p.supplier_id AS partner_id, bp.legal_name AS partner_name, p.purchase_number AS document_number,
                   p.number_code, p.status, p.currency_code, p.total_amount, p.payment_status, p.paid_amount, p.confirmed_at, p.cancelled_at
            FROM purchase_orders p
            JOIN business_partners bp ON bp.tenant_id = p.tenant_id AND bp.id = p.supplier_id
            WHERE p.tenant_id = ?
              AND p.deleted_at IS NULL
              AND p.status IN ('CONFIRMED', 'CANCELLED')
              AND p.confirmed_at >= ?
              AND p.confirmed_at < ?
            ORDER BY p.confirmed_at DESC, p.purchase_number DESC
            LIMIT ?
            """;
        List<CommercialDocumentReportRow> rows = jdbcTemplate.query(sql, this::mapCommercialDocumentRow, tenantId.value(), timestamp(range.from()),
            timestamp(range.to()), range.limit());
        return new PurchasesRangeReport(new ReportPeriod(range.from(), range.to()), summary, rows);
    }

    @Override
    public CashMovementsRangeReport cashMovementsByRange(TenantId tenantId, ReportRange range, String currencyCode) {
        String summarySql = """
            SELECT COUNT(*) AS movement_count,
                   COALESCE(SUM(CASE WHEN cash_effect > 0 THEN cash_effect ELSE 0 END), 0) AS total_cash_in,
                   COALESCE(SUM(CASE WHEN cash_effect < 0 THEN abs(cash_effect) ELSE 0 END), 0) AS total_cash_out,
                   COALESCE(SUM(cash_effect), 0) AS net_cash_effect
            FROM cash_movements
            WHERE tenant_id = ?
              AND occurred_at >= ?
              AND occurred_at < ?
            """;
        CashMovementReportSummary summary = jdbcTemplate.query(summarySql, (rs, rowNum) -> new CashMovementReportSummary(
            currencyCode,
            rs.getLong("movement_count"),
            money(rs, "total_cash_in"),
            money(rs, "total_cash_out"),
            money(rs, "net_cash_effect")
        ), tenantId.value(), timestamp(range.from()), timestamp(range.to())).getFirst();

        String rowsSql = """
            SELECT id, cash_session_id, cash_register_id, branch_id, movement_type, payment_method, amount, cash_effect,
                   currency_code, reference_type, reference_id, created_by, occurred_at
            FROM cash_movements
            WHERE tenant_id = ?
              AND occurred_at >= ?
              AND occurred_at < ?
            ORDER BY occurred_at DESC
            LIMIT ?
            """;
        List<CashMovementReportRow> rows = jdbcTemplate.query(rowsSql, this::mapCashMovementRow, tenantId.value(), timestamp(range.from()),
            timestamp(range.to()), range.limit());
        return new CashMovementsRangeReport(new ReportPeriod(range.from(), range.to()), summary, rows);
    }

    @Override
    public TopProductsSoldReport topProductsSold(TenantId tenantId, ReportRange range, String currencyCode) {
        String sql = """
            SELECT i.product_id, i.product_sku, i.product_name, s.currency_code,
                   COALESCE(SUM(i.quantity), 0) AS quantity_sold,
                   COALESCE(SUM(i.subtotal_amount), 0) AS total_amount,
                   COUNT(DISTINCT s.id) AS sales_count
            FROM sales_order_items i
            JOIN sales_orders s ON s.tenant_id = i.tenant_id AND s.id = i.sale_id
            WHERE s.tenant_id = ?
              AND s.deleted_at IS NULL
              AND s.status = 'CONFIRMED'
              AND s.confirmed_at >= ?
              AND s.confirmed_at < ?
            GROUP BY i.product_id, i.product_sku, i.product_name, s.currency_code
            ORDER BY quantity_sold DESC, total_amount DESC, i.product_name ASC
            LIMIT ?
            """;
        List<TopProductSoldReportRow> rows = jdbcTemplate.query(sql, this::mapTopProductSoldRow, tenantId.value(), timestamp(range.from()),
            timestamp(range.to()), range.limit());
        return new TopProductsSoldReport(new ReportPeriod(range.from(), range.to()), rows);
    }

    @Override
    public LowStockReport lowStock(TenantId tenantId, BigDecimal threshold, int limit) {
        String sql = """
            SELECT sb.id AS stock_balance_id, sb.warehouse_id, w.code AS warehouse_code, w.name AS warehouse_name,
                   sb.product_id, p.sku AS product_sku, p.name AS product_name, sb.quantity_on_hand, sb.reserved_quantity,
                   (sb.quantity_on_hand - sb.reserved_quantity) AS available_quantity, sb.updated_at
            FROM stock_balances sb
            JOIN products p ON p.tenant_id = sb.tenant_id AND p.id = sb.product_id
            JOIN warehouses w ON w.tenant_id = sb.tenant_id AND w.id = sb.warehouse_id
            WHERE sb.tenant_id = ?
              AND p.deleted_at IS NULL
              AND p.status = 'ACTIVE'
              AND p.product_type = 'GOOD'
              AND p.stock_tracking_enabled = true
              AND w.deleted_at IS NULL
              AND w.is_active = true
              AND sb.quantity_on_hand <= ?
            ORDER BY sb.quantity_on_hand ASC, p.name ASC, w.name ASC
            LIMIT ?
            """;
        List<LowStockReportRow> rows = jdbcTemplate.query(sql, this::mapLowStockRow, tenantId.value(), threshold, limit);
        return new LowStockReport(threshold, rows);
    }

    @Override
    public CommercialReportSummary summarizeSales(TenantId tenantId, Instant from, Instant to, String currencyCode) {
        String sql = """
            SELECT COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_count,
                   COALESCE(SUM(CASE WHEN status = 'CONFIRMED' THEN total_amount ELSE 0 END), 0) AS confirmed_total_amount,
                   COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_count,
                   COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN total_amount ELSE 0 END), 0) AS cancelled_total_amount,
                   COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND payment_status = 'PAID') AS paid_count,
                   COALESCE(SUM(CASE WHEN status = 'CONFIRMED' AND payment_status = 'PAID' THEN paid_amount ELSE 0 END), 0) AS paid_total_amount
            FROM sales_orders
            WHERE tenant_id = ?
              AND deleted_at IS NULL
              AND status IN ('CONFIRMED', 'CANCELLED')
              AND confirmed_at >= ?
              AND confirmed_at < ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCommercialSummary(rs, currencyCode), tenantId.value(), timestamp(from), timestamp(to)).getFirst();
    }

    @Override
    public CommercialReportSummary summarizePurchases(TenantId tenantId, Instant from, Instant to, String currencyCode) {
        String sql = """
            SELECT COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_count,
                   COALESCE(SUM(CASE WHEN status = 'CONFIRMED' THEN total_amount ELSE 0 END), 0) AS confirmed_total_amount,
                   COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_count,
                   COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN total_amount ELSE 0 END), 0) AS cancelled_total_amount,
                   COUNT(*) FILTER (WHERE status = 'CONFIRMED' AND payment_status = 'PAID') AS paid_count,
                   COALESCE(SUM(CASE WHEN status = 'CONFIRMED' AND payment_status = 'PAID' THEN paid_amount ELSE 0 END), 0) AS paid_total_amount
            FROM purchase_orders
            WHERE tenant_id = ?
              AND deleted_at IS NULL
              AND status IN ('CONFIRMED', 'CANCELLED')
              AND confirmed_at >= ?
              AND confirmed_at < ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapCommercialSummary(rs, currencyCode), tenantId.value(), timestamp(from), timestamp(to)).getFirst();
    }

    @Override
    public Optional<CashSessionDashboard> findCurrentCashSession(TenantId tenantId, UUID userId) {
        String sql = """
            SELECT cs.id, cs.cash_register_id, cr.code AS cash_register_code, cr.name AS cash_register_name, cs.branch_id,
                   cs.status, cs.opening_amount, (cs.opening_amount + COALESCE(SUM(cm.cash_effect), 0)) AS expected_closing_amount,
                   cs.currency_code, cs.opened_at, cs.updated_at
            FROM cash_sessions cs
            JOIN cash_registers cr ON cr.tenant_id = cs.tenant_id AND cr.id = cs.cash_register_id
            LEFT JOIN cash_movements cm ON cm.tenant_id = cs.tenant_id AND cm.cash_session_id = cs.id
            WHERE cs.tenant_id = ?
              AND cs.opened_by_user_id = ?
              AND cs.status = 'OPEN'
            GROUP BY cs.id, cs.cash_register_id, cr.code, cr.name, cs.branch_id, cs.status, cs.opening_amount,
                     cs.currency_code, cs.opened_at, cs.updated_at
            ORDER BY cs.opened_at DESC
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, this::mapCashSessionDashboard, tenantId.value(), userId).stream().findFirst();
    }

    @Override
    public List<StockMovementDashboardRow> latestStockMovements(TenantId tenantId, int limit) {
        String sql = """
            SELECT sm.id, sm.warehouse_id, w.code AS warehouse_code, w.name AS warehouse_name, sm.product_id,
                   p.sku AS product_sku, p.name AS product_name, sm.movement_type, sm.quantity, sm.reference_type, sm.reference_id, sm.confirmed_at
            FROM stock_movements sm
            JOIN products p ON p.tenant_id = sm.tenant_id AND p.id = sm.product_id
            JOIN warehouses w ON w.tenant_id = sm.tenant_id AND w.id = sm.warehouse_id
            WHERE sm.tenant_id = ?
            ORDER BY sm.confirmed_at DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, this::mapStockMovementDashboardRow, tenantId.value(), limit);
    }

    private CommercialReportSummary mapCommercialSummary(ResultSet rs, String currencyCode) throws SQLException {
        return new CommercialReportSummary(
            currencyCode,
            rs.getLong("confirmed_count"),
            money(rs, "confirmed_total_amount"),
            rs.getLong("cancelled_count"),
            money(rs, "cancelled_total_amount"),
            rs.getLong("paid_count"),
            money(rs, "paid_total_amount")
        );
    }

    private CommercialDocumentReportRow mapCommercialDocumentRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
        return new CommercialDocumentReportRow(
            rs.getObject("id", UUID.class),
            rs.getObject("branch_id", UUID.class),
            rs.getObject("partner_id", UUID.class),
            rs.getString("partner_name"),
            rs.getLong("document_number"),
            rs.getString("number_code"),
            rs.getString("status"),
            rs.getString("currency_code"),
            money(rs, "total_amount"),
            rs.getString("payment_status"),
            money(rs, "paid_amount"),
            rs.getTimestamp("confirmed_at").toInstant(),
            cancelledAt == null ? null : cancelledAt.toInstant()
        );
    }

    private CashMovementReportRow mapCashMovementRow(ResultSet rs, int rowNum) throws SQLException {
        return new CashMovementReportRow(
            rs.getObject("id", UUID.class),
            rs.getObject("cash_session_id", UUID.class),
            rs.getObject("cash_register_id", UUID.class),
            rs.getObject("branch_id", UUID.class),
            rs.getString("movement_type"),
            rs.getString("payment_method"),
            money(rs, "amount"),
            money(rs, "cash_effect"),
            rs.getString("currency_code"),
            rs.getString("reference_type"),
            rs.getObject("reference_id", UUID.class),
            rs.getObject("created_by", UUID.class),
            rs.getTimestamp("occurred_at").toInstant()
        );
    }

    private TopProductSoldReportRow mapTopProductSoldRow(ResultSet rs, int rowNum) throws SQLException {
        return new TopProductSoldReportRow(
            rs.getObject("product_id", UUID.class),
            rs.getString("product_sku"),
            rs.getString("product_name"),
            quantity(rs, "quantity_sold"),
            rs.getString("currency_code"),
            money(rs, "total_amount"),
            rs.getLong("sales_count")
        );
    }

    private LowStockReportRow mapLowStockRow(ResultSet rs, int rowNum) throws SQLException {
        return new LowStockReportRow(
            rs.getObject("stock_balance_id", UUID.class),
            rs.getObject("warehouse_id", UUID.class),
            rs.getString("warehouse_code"),
            rs.getString("warehouse_name"),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_sku"),
            rs.getString("product_name"),
            quantity(rs, "quantity_on_hand"),
            quantity(rs, "reserved_quantity"),
            quantity(rs, "available_quantity"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private CashSessionDashboard mapCashSessionDashboard(ResultSet rs, int rowNum) throws SQLException {
        return new CashSessionDashboard(
            rs.getObject("id", UUID.class),
            rs.getObject("cash_register_id", UUID.class),
            rs.getString("cash_register_code"),
            rs.getString("cash_register_name"),
            rs.getObject("branch_id", UUID.class),
            rs.getString("status"),
            money(rs, "opening_amount"),
            money(rs, "expected_closing_amount"),
            rs.getString("currency_code"),
            rs.getTimestamp("opened_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private StockMovementDashboardRow mapStockMovementDashboardRow(ResultSet rs, int rowNum) throws SQLException {
        return new StockMovementDashboardRow(
            rs.getObject("id", UUID.class),
            rs.getObject("warehouse_id", UUID.class),
            rs.getString("warehouse_code"),
            rs.getString("warehouse_name"),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_sku"),
            rs.getString("product_name"),
            rs.getString("movement_type"),
            quantity(rs, "quantity"),
            rs.getString("reference_type"),
            rs.getObject("reference_id", UUID.class),
            rs.getTimestamp("confirmed_at").toInstant()
        );
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private BigDecimal money(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private BigDecimal quantity(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return (value == null ? BigDecimal.ZERO : value).setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }
}