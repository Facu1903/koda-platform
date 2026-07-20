package com.koda.platform.platform.purchases.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.purchases.application.PreparedPurchaseDraft;
import com.koda.platform.platform.purchases.application.PreparedPurchaseItem;
import com.koda.platform.platform.purchases.application.Purchase;
import com.koda.platform.platform.purchases.application.PurchaseItem;
import com.koda.platform.platform.purchases.application.PurchaseItemStockReversal;
import com.koda.platform.platform.purchases.application.PurchaseItemStockUpdate;
import com.koda.platform.platform.purchases.application.PurchasePaymentReversalUpdate;
import com.koda.platform.platform.purchases.application.PurchasePaymentUpdate;
import com.koda.platform.platform.purchases.application.PurchaseProduct;
import com.koda.platform.platform.purchases.application.PurchaseSupplier;
import com.koda.platform.platform.purchases.application.PurchasesRepository;
import com.koda.platform.platform.purchases.application.PurchasesRequestMetadata;
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
@ConditionalOnProperty(prefix = "koda.purchases.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcPurchasesRepository implements PurchasesRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcPurchasesRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Purchase> listPurchases(TenantId tenantId, int limit) {
        String sql = purchaseSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY updated_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapPurchaseHeader, tenantId.value(), limit).stream()
            .map(header -> toPurchase(header, listItems(tenantId, header.id())))
            .toList();
    }

    @Override
    public Optional<Purchase> findById(TenantId tenantId, UUID id) {
        String sql = purchaseSelect() + " WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL";
        return jdbcTemplate.query(sql, this::mapPurchaseHeader, tenantId.value(), id).stream()
            .findFirst()
            .map(header -> toPurchase(header, listItems(tenantId, header.id())));
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
    public Optional<PurchaseSupplier> findActiveSupplier(TenantId tenantId, UUID id) {
        String sql = supplierSelect() + " AND p.id = ?";
        return jdbcTemplate.query(sql, this::mapSupplier, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Optional<PurchaseProduct> findPurchasableProduct(TenantId tenantId, UUID id) {
        String sql = """
            SELECT id, sku, name, product_type, status, stock_tracking_enabled, allow_negative_stock
            FROM products
            WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, this::mapProduct, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Purchase createDraft(TenantId tenantId, UUID actorUserId, PreparedPurchaseDraft draft) {
        long purchaseNumber = nextPurchaseNumber(tenantId, draft.branchId());
        String numberCode = "CMP-%08d".formatted(purchaseNumber);
        String sql = """
            INSERT INTO purchase_orders (
                tenant_id, branch_id, supplier_id, purchase_number, number_code, supplier_document_number, status, currency_code,
                subtotal_amount, total_amount, created_by, updated_by
            ) VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?)
            RETURNING id
            """;
        UUID id = jdbcTemplate.queryForObject(sql, UUID.class, tenantId.value(), draft.branchId(), draft.supplierId(), purchaseNumber, numberCode,
            draft.supplierDocumentNumber(), draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), actorUserId, actorUserId);
        insertItems(tenantId, id, draft.items());
        return findById(tenantId, id).orElseThrow();
    }

    @Override
    public Optional<Purchase> replaceDraft(TenantId tenantId, UUID id, UUID actorUserId, long version, PreparedPurchaseDraft draft) {
        String sql = """
            UPDATE purchase_orders
            SET branch_id = ?, supplier_id = ?, supplier_document_number = ?, currency_code = ?, subtotal_amount = ?, total_amount = ?,
                updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'DRAFT' AND deleted_at IS NULL
            RETURNING id
            """;
        Optional<UUID> updated = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), draft.branchId(), draft.supplierId(),
            draft.supplierDocumentNumber(), draft.currencyCode(), draft.subtotalAmount(), draft.totalAmount(), actorUserId, tenantId.value(), id,
            version).stream().findFirst();
        if (updated.isEmpty()) {
            return Optional.empty();
        }
        jdbcTemplate.update("DELETE FROM purchase_order_items WHERE tenant_id = ? AND purchase_id = ?", tenantId.value(), id);
        insertItems(tenantId, id, draft.items());
        return findById(tenantId, id);
    }

    @Override
    public boolean softDeleteDraft(TenantId tenantId, UUID id, UUID actorUserId, long version) {
        String sql = """
            UPDATE purchase_orders
            SET deleted_at = now(), deleted_by = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'DRAFT' AND deleted_at IS NULL
            """;
        return jdbcTemplate.update(sql, actorUserId, actorUserId, tenantId.value(), id, version) == 1;
    }

    @Override
    public Optional<Purchase> confirmPurchase(TenantId tenantId, UUID id, UUID actorUserId, long version, List<PurchaseItemStockUpdate> stockUpdates,
                                              PurchasePaymentUpdate paymentUpdate) {
        for (PurchaseItemStockUpdate update : stockUpdates) {
            jdbcTemplate.update("""
                UPDATE purchase_order_items
                SET stock_movement_id = ?
                WHERE tenant_id = ? AND purchase_id = ? AND id = ?
                """, update.stockMovementId(), tenantId.value(), id, update.purchaseItemId());
        }
        String paymentStatus = paymentUpdate == null ? "UNPAID" : "PAID";
        String sql = """
            UPDATE purchase_orders
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
    public Optional<Purchase> cancelPurchase(TenantId tenantId, UUID id, UUID actorUserId, long version, String reason,
                                             List<PurchaseItemStockReversal> stockReversals, PurchasePaymentReversalUpdate paymentReversal) {
        for (PurchaseItemStockReversal reversal : stockReversals) {
            jdbcTemplate.update("""
                UPDATE purchase_order_items
                SET stock_reversal_movement_id = ?
                WHERE tenant_id = ? AND purchase_id = ? AND id = ?
                """, reversal.stockReversalMovementId(), tenantId.value(), id, reversal.purchaseItemId());
        }
        String sql;
        Object[] args;
        if (paymentReversal == null) {
            sql = """
                UPDATE purchase_orders
                SET status = 'CANCELLED', cancelled_at = now(), cancelled_by = ?, cancellation_reason = ?, updated_at = now(), updated_by = ?,
                    version = version + 1
                WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'CONFIRMED' AND deleted_at IS NULL
                RETURNING id
                """;
            args = new Object[] {actorUserId, reason, actorUserId, tenantId.value(), id, version};
        } else {
            sql = """
                UPDATE purchase_orders
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
                                 String outcome, PurchasesRequestMetadata metadata, Map<String, Object> details) {
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

    private long nextPurchaseNumber(TenantId tenantId, UUID branchId) {
        String sql = """
            WITH upsert AS (
                INSERT INTO purchase_number_sequences (tenant_id, branch_id, next_number)
                VALUES (?, ?, 2)
                ON CONFLICT (tenant_id, branch_id)
                DO UPDATE SET next_number = purchase_number_sequences.next_number + 1, updated_at = now()
                RETURNING next_number
            )
            SELECT next_number - 1 FROM upsert
            """;
        Long value = jdbcTemplate.queryForObject(sql, Long.class, tenantId.value(), branchId);
        return value == null ? 1 : value;
    }

    private void insertItems(TenantId tenantId, UUID purchaseId, List<PreparedPurchaseItem> items) {
        String sql = """
            INSERT INTO purchase_order_items (
                tenant_id, purchase_id, line_number, product_id, warehouse_id, product_sku, product_name, product_type,
                stock_tracking_enabled, quantity, unit_cost, subtotal_amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        for (PreparedPurchaseItem item : items) {
            jdbcTemplate.update(sql, tenantId.value(), purchaseId, item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(),
                item.productName(), item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitCost(), item.subtotalAmount());
        }
    }

    private List<PurchaseItem> listItems(TenantId tenantId, UUID purchaseId) {
        String sql = """
            SELECT id, purchase_id, line_number, product_id, warehouse_id, product_sku, product_name, product_type, stock_tracking_enabled,
                   quantity, unit_cost, subtotal_amount, stock_movement_id, stock_reversal_movement_id
            FROM purchase_order_items
            WHERE tenant_id = ? AND purchase_id = ?
            ORDER BY line_number
            """;
        return jdbcTemplate.query(sql, this::mapItem, tenantId.value(), purchaseId);
    }

    private String supplierSelect() {
        return """
            SELECT p.id, p.legal_name, p.status, p.is_system
            FROM business_partners p
            JOIN business_partner_roles r ON r.tenant_id = p.tenant_id AND r.business_partner_id = p.id
            WHERE p.tenant_id = ?
              AND p.status = 'ACTIVE'
              AND p.deleted_at IS NULL
              AND r.role_type = 'SUPPLIER'
              AND r.deleted_at IS NULL
            """;
    }

    private String purchaseSelect() {
        return """
            SELECT id, tenant_id, branch_id, supplier_id, purchase_number, number_code, supplier_document_number, status, currency_code,
                   subtotal_amount, total_amount, payment_status, payment_method, paid_amount, cash_session_id, cash_movement_id,
                   payment_reversal_cash_session_id, payment_reversal_cash_movement_id, confirmed_at, confirmed_by, cancelled_at, cancelled_by,
                   cancellation_reason, version, updated_at
            FROM purchase_orders
            """;
    }

    private PurchaseHeader mapPurchaseHeader(ResultSet rs, int rowNum) throws SQLException {
        return new PurchaseHeader(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("branch_id", UUID.class),
            rs.getObject("supplier_id", UUID.class),
            rs.getLong("purchase_number"),
            rs.getString("number_code"),
            rs.getString("supplier_document_number"),
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

    private PurchaseItem mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new PurchaseItem(
            rs.getObject("id", UUID.class),
            rs.getObject("purchase_id", UUID.class),
            rs.getInt("line_number"),
            rs.getObject("product_id", UUID.class),
            rs.getObject("warehouse_id", UUID.class),
            rs.getString("product_sku"),
            rs.getString("product_name"),
            rs.getString("product_type"),
            rs.getBoolean("stock_tracking_enabled"),
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("unit_cost"),
            rs.getBigDecimal("subtotal_amount"),
            rs.getObject("stock_movement_id", UUID.class),
            rs.getObject("stock_reversal_movement_id", UUID.class)
        );
    }

    private PurchaseSupplier mapSupplier(ResultSet rs, int rowNum) throws SQLException {
        return new PurchaseSupplier(rs.getObject("id", UUID.class), rs.getString("legal_name"), rs.getString("status"), rs.getBoolean("is_system"));
    }

    private PurchaseProduct mapProduct(ResultSet rs, int rowNum) throws SQLException {
        return new PurchaseProduct(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getString("name"), rs.getString("product_type"),
            rs.getString("status"), rs.getBoolean("stock_tracking_enabled"), rs.getBoolean("allow_negative_stock"));
    }

    private Purchase toPurchase(PurchaseHeader header, List<PurchaseItem> items) {
        return new Purchase(header.id(), header.tenantId(), header.branchId(), header.supplierId(), header.purchaseNumber(), header.numberCode(),
            header.supplierDocumentNumber(), header.status(), header.currencyCode(), header.subtotalAmount(), header.totalAmount(),
            header.paymentStatus(), header.paymentMethod(), header.paidAmount(), header.cashSessionId(), header.cashMovementId(),
            header.paymentReversalCashSessionId(), header.paymentReversalCashMovementId(), header.confirmedAt(), header.confirmedBy(),
            header.cancelledAt(), header.cancelledBy(), header.cancellationReason(), header.version(), header.updatedAt(), List.copyOf(items));
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

    private record PurchaseHeader(
        UUID id,
        TenantId tenantId,
        UUID branchId,
        UUID supplierId,
        long purchaseNumber,
        String numberCode,
        String supplierDocumentNumber,
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