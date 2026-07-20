package com.koda.platform.platform.sales.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.sales.application.PreparedSaleDraft;
import com.koda.platform.platform.sales.application.PreparedSaleItem;
import com.koda.platform.platform.sales.application.Sale;
import com.koda.platform.platform.sales.application.SaleItem;
import com.koda.platform.platform.sales.application.SaleItemStockReversal;
import com.koda.platform.platform.sales.application.SaleItemStockUpdate;
import com.koda.platform.platform.sales.application.SalePaymentReversalUpdate;
import com.koda.platform.platform.sales.application.SalePaymentUpdate;
import com.koda.platform.platform.sales.application.SalesCustomer;
import com.koda.platform.platform.sales.application.SalesProduct;
import com.koda.platform.platform.sales.application.SalesRepository;
import com.koda.platform.platform.sales.application.SalesRequestMetadata;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.sales.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSalesRepository implements SalesRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcSalesRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Sale> listSales(TenantId tenantId, int limit) {
        String sql = saleSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapSaleHeader, tenantId.value(), limit).stream()
            .map(header -> toSale(header, listItems(tenantId, header.id())))
            .toList();
    }

    @Override
    public Optional<Sale> findById(TenantId tenantId, UUID id) {
        String sql = saleSelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL";
        return jdbcTemplate.query(sql, this::mapSaleHeader, tenantId.value(), id).stream()
            .findFirst()
            .map(header -> toSale(header, listItems(tenantId, header.id())));
    }

    @Override
    public Optional<String> findTenantCurrency(TenantId tenantId) {
        return jdbcTemplate.query("SELECT default_currency FROM tenants WHERE id = ?", (rs, rowNum) -> rs.getString("default_currency"), tenantId.value())
            .stream()
            .findFirst();
    }

    @Override
    public boolean existsActiveBranch(TenantId tenantId, UUID branchId) {
        String sql = "SELECT count(*) FROM branches WHERE tenant_id = ? AND id = ? AND is_active = true AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId.value(), branchId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsActiveWarehouse(TenantId tenantId, UUID branchId, UUID warehouseId) {
        String sql = "SELECT count(*) FROM warehouses WHERE tenant_id = ? AND branch_id = ? AND id = ? AND is_active = true AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId.value(), branchId, warehouseId);
        return count != null && count > 0;
    }

    @Override
    public Optional<SalesCustomer> findActiveCustomer(TenantId tenantId, UUID id) {
        String sql = customerSelect() + " AND p.id = ?";
        return jdbcTemplate.query(sql, this::mapCustomer, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Optional<SalesCustomer> findDefaultCustomer(TenantId tenantId) {
        String sql = customerSelect() + " AND p.is_system = true ORDER BY p.created_at ASC LIMIT 1";
        return jdbcTemplate.query(sql, this::mapCustomer, tenantId.value()).stream().findFirst();
    }

    @Override
    public Optional<SalesProduct> findSellableProduct(TenantId tenantId, UUID id) {
        String sql = """
            SELECT id, sku, name, product_type, status, stock_tracking_enabled, allow_negative_stock
            FROM products
            WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, this::mapProduct, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Sale createDraft(TenantId tenantId, UUID actorUserId, PreparedSaleDraft draft) {
        long saleNumber = nextSaleNumber(tenantId, draft.branchId());
        String numberCode = "VTA-%08d".formatted(saleNumber);
        String sql = """
            INSERT INTO sales_orders (
                tenant_id, branch_id, customer_id, sale_number, number_code, status, currency_code, subtotal_amount, total_amount,
                created_by, updated_by
            ) VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?)
            RETURNING id
            """;
        UUID id = jdbcTemplate.queryForObject(sql, UUID.class, tenantId.value(), draft.branchId(), draft.customerId(), saleNumber, numberCode,
            draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), actorUserId, actorUserId);
        insertItems(tenantId, id, draft.items());
        return findById(tenantId, id).orElseThrow();
    }

    @Override
    public Optional<Sale> replaceDraft(TenantId tenantId, UUID id, UUID actorUserId, long version, PreparedSaleDraft draft) {
        String sql = """
            UPDATE sales_orders
            SET branch_id = ?, customer_id = ?, currency_code = ?, subtotal_amount = ?, total_amount = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'DRAFT' AND deleted_at IS NULL
            RETURNING id
            """;
        Optional<UUID> updated = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), draft.branchId(), draft.customerId(),
            draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), actorUserId, tenantId.value(), id, version).stream().findFirst();
        if (updated.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update("DELETE FROM sales_order_items WHERE tenant_id = ? AND sale_id = ?", tenantId.value(), id);
        insertItems(tenantId, id, draft.items());
        return findById(tenantId, id);
    }

    @Override
    public boolean softDeleteDraft(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        String sql = """
            UPDATE sales_orders
            SET deleted_at = now(), deleted_by = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'DRAFT' AND deleted_at IS NULL
            """;
        return jdbcTemplate.update(sql, actorUserId, actorUserId, tenantId.value(), id, version) == 1;
    }

    @Override
    public Optional<Sale> confirmSale(TenantId tenantId, UUID id, UUID actorUserId, long version, List<SaleItemStockUpdate> stockUpdates,
                                      SalePaymentUpdate paymentUpdate) {
        for (SaleItemStockUpdate update : stockUpdates) {
            jdbcTemplate.update("""
                UPDATE sales_order_items
                SET stock_movement_id = ?
                WHERE tenant_id = ? AND sale_id = ? AND id = ?
                """, update.stockMovementId(), tenantId.value(), id, update.saleItemId());
        }
        String paymentStatus = paymentUpdate == null ? "UNPAID" : "PAID";
        String sql = """
            UPDATE sales_orders
            SET status = 'CONFIRMED', confirmed_at = now(), confirmed_by = ?, payment_status = ?, payment_method = ?, paid_amount = ?,
                cash_session_id = ?, cash_movement_id = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'DRAFT' AND deleted_at IS NULL
            RETURNING id
            """;
        Optional<UUID> updated = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), actorUserId, paymentStatus,
            paymentUpdate == null ? null : paymentUpdate.paymentMethod(),
            paymentUpdate == null ? BigDecimal.ZERO.setScale(4) : paymentUpdate.paidAmount(),
            paymentUpdate == null ? null : paymentUpdate.cashSessionId(),
            paymentUpdate == null ? null : paymentUpdate.cashMovementId(),
            actorUserId, tenantId.value(), id, version).stream().findFirst();
        return updated.isEmpty() ? Optional.empty() : findById(tenantId, id);
    }

    @Override
    public Optional<Sale> cancelSale(TenantId tenantId, UUID id, UUID actorUserId, long version, String reason,
                                     List<SaleItemStockReversal> stockReversals, SalePaymentReversalUpdate paymentReversal) {
        for (SaleItemStockReversal reversal : stockReversals) {
            jdbcTemplate.update("""
                UPDATE sales_order_items
                SET stock_reversal_movement_id = ?
                WHERE tenant_id = ? AND sale_id = ? AND id = ?
                """, reversal.stockReversalMovementId(), tenantId.value(), id, reversal.saleItemId());
        }
        String sql;
        Object[] args;
        if (paymentReversal == null) {
            sql = """
                UPDATE sales_orders
                SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ?, cancellation_reason = ?, updated_at = now(), updated_by = ?,
                    version = version + 1
                WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'CONFIRMED' AND deleted_at IS NULL
                RETURNING id
                """;
            args = new Object[] {actorUserId, reason, actorUserId, tenantId.value(), id, version};
        } else {
            sql = """
                UPDATE sales_orders
                SET status = 'CANCELLED', payment_status = 'REVERSED', payment_reversal_cash_session_id = ?, payment_reversal_cash_movement_id = ?,
                    cancelled_at = now(), cancelled_by = ?, cancellation_reason = ?, updated_at = now(), updated_by = ?, version = version + 1
                WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'CONFIRMED' AND payment_status = 'PAID' AND deleted_at IS NULL
                RETURNING id
                """;
            args = new Object[] {paymentReversal.cashSessionId(), paymentReversal.cashMovementId(), actorUserId, reason, actorUserId,
                tenantId.value(), id, version};
        }
        Optional<UUID> updated = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), args).stream().findFirst();
        return updated.isEmpty() ? Optional.empty() : findById(tenantId, id);
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                 String outcome, SalesRequestMetadata metadata, Map<String, Object> details) {
        String sql = """
            INSERT INTO audit_events (
                tenant_id, actor_user_id, actor_type, action, resource_type, resource_id, outcome, source_ip, user_agent, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS inet), ?, CAST(? AS jsonb))
            """;
        jdbcTemplate.update(sql,
            tenantId == null ? null : tenantId.value(),
            actorUserId,
            actorUserId == null ? "SYSTEM" : "USER",
            action,
            resourceType,
            resourceId,
            outcome,
            metadata == null ? null : metadata.sourceIp(),
            metadata == null ? null : metadata.userAgent(),
            toJson(details)
        );
    }

    private long nextSaleNumber(TenantId tenantId, UUID branchId) {
        String sql = """
            WITH upsert AS (
                INSERT INTO sales_number_sequences (tenant_id, branch_id, next_number)
                VALUES (?, ?, 2)
                ON CONFLICT (tenant_id, branch_id)
                DO UPDATE SET next_number = sales_number_sequences.next_number + 1, updated_at = now()
                RETURNING next_number
            )
            SELECT next_number - 1 FROM upsert
            """;
        Long value = jdbcTemplate.queryForObject(sql, Long.class, tenantId.value(), branchId);
        return value == null ? 1 : value;
    }

    private void insertItems(TenantId tenantId, UUID saleId, List<PreparedSaleItem> items) {
        String sql = """
            INSERT INTO sales_order_items (
                tenant_id, sale_id, line_number, product_id, warehouse_id, product_sku, product_name, product_type,
                stock_tracking_enabled, quantity, unit_price, subtotal_amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        for (PreparedSaleItem item : items) {
            jdbcTemplate.update(sql, tenantId.value(), saleId, item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(),
                item.productName(), item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitPrice(), item.subtotalAmount());
        }
    }

    private List<SaleItem> listItems(TenantId tenantId, UUID saleId) {
        String sql = """
            SELECT id, sale_id, line_number, product_id, warehouse_id, product_sku, product_name, product_type, stock_tracking_enabled,
                   quantity, unit_price, subtotal_amount, stock_movement_id, stock_reversal_movement_id
            FROM sales_order_items
            WHERE tenant_id = ? AND sale_id = ?
            ORDER BY line_number
            """;
        return jdbcTemplate.query(sql, this::mapItem, tenantId.value(), saleId);
    }

    private String customerSelect() {
        return """
            SELECT p.id, p.legal_name, p.status, p.is_system
            FROM business_partners p
            JOIN business_partner_roles r ON r.tenant_id = p.tenant_id AND r.business_partner_id = p.id
            WHERE p.tenant_id = ?
              AND p.status = 'ACTIVE'
              AND p.deleted_at IS NULL
              AND r.role_type = 'CUSTOMER'
              AND r.deleted_at IS NULL
            """;
    }

    private String saleSelect() {
        return """
            SELECT id, tenant_id, branch_id, customer_id, sale_number, number_code, status, currency_code, subtotal_amount, total_amount,
                   payment_status, payment_method, paid_amount, cash_session_id, cash_movement_id, payment_reversal_cash_session_id,
                   payment_reversal_cash_movement_id, confirmed_at, confirmed_by, cancelled_at, cancelled_by, cancellation_reason, version, updated_at
            FROM sales_orders
            """;
    }

    private SaleHeader mapSaleHeader(ResultSet rs, int rowNum) throws SQLException {
        return new SaleHeader(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("branch_id", UUID.class),
            rs.getObject("customer_id", UUID.class),
            rs.getLong("sale_number"),
            rs.getString("number_code"),
            rs.getString("status"),
            rs.getString("currency_code"),
            rs.getBigDecimal("subtotal_amount"),
            rs.getBigDecimal("total_amount"),
            rs.getString("payment_status"),
            rs.getString("payment_method"),
            rs.getBigDecimal("paid_amount"),
            rs.getObject("cash_session_id", UUID.class),
            rs.getObject("cash_movement_id", UUID.class),
            rs.getObject("payment_reversal_cash_session_id", UUID.class),
            rs.getObject("payment_reversal_cash_movement_id", UUID.class),
            instantOrNull(rs.getTimestamp("confirmed_at")),
            rs.getObject("confirmed_by", UUID.class),
            instantOrNull(rs.getTimestamp("cancelled_at")),
            rs.getObject("cancelled_by", UUID.class),
            rs.getString("cancellation_reason"),
            rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private SaleItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new SaleItem(
            rs.getObject("id", UUID.class),
            rs.getObject("sale_id", UUID.class),
            rs.getInt("line_number"),
            rs.getObject("product_id", UUID.class),
            rs.getObject("warehouse_id", UUID.class),
            rs.getString("product_sku"),
            rs.getString("product_name"),
            rs.getString("product_type"),
            rs.getBoolean("stock_tracking_enabled"),
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("unit_price"),
            rs.getBigDecimal("subtotal_amount"),
            rs.getObject("stock_movement_id", UUID.class),
            rs.getObject("stock_reversal_movement_id", UUID.class)
        );
    }

    private SalesCustomer mapCustomer(ResultSet rs, int rowNum) throws SQLException {
        return new SalesCustomer(rs.getObject("id", UUID.class), rs.getString("legal_name"), rs.getString("status"), rs.getBoolean("is_system"));
    }

    private SalesProduct mapProduct(ResultSet rs, int rowNum) throws SQLException {
        return new SalesProduct(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("name"), rs.getString("product_type"),
            rs.getString("status"), rs.getBoolean("stock_tracking_enabled"), rs.getBoolean("allow_negative_stock"));
    }

    private Sale toSale(SaleHeader header, List<SaleItem> items) {
        return new Sale(header.id(), header.tenantId(), header.branchId(), header.customerId(), header.saleNumber(), header.numberCode(), header.status(),
            header.currencyCode(), header.subtotalAmount(), header.totalAmount(), header.paymentStatus(), header.paymentMethod(), header.paidAmount(),
            header.cashSessionId(), header.cashMovementId(), header.paymentReversalCashSessionId(), header.paymentReversalCashMovementId(),
            header.confirmedAt(), header.confirmedBy(), header.cancelledAt(), header.cancelledBy(), header.cancellationReason(), header.version(),
            header.updatedAt(), List.copyOf(items));
    }

    private Instant instantOrNull(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }

    private record SaleHeader(
        UUID id,
        TenantId tenantId,
        UUID branchId,
        UUID customerId,
        long saleNumber,
        String numberCode,
        String status,
        String currencyCode,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String paymentMethod,
        BigDecimal paidAmount,
        UUID cashSessionId,
        UUID cashMovementId,
        UUID paymentReversalCashSessionId,
        UUID paymentReversalCashMovementId,
        Instant confirmedAt,
        UUID confirmedBy,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancellationReason,
        long version,
        Instant updatedAt
    ) {
    }
}