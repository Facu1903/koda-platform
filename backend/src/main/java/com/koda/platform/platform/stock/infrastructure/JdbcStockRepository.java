package com.koda.platform.platform.stock.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.stock.application.CreateStockMovementCommand;
import com.koda.platform.platform.stock.application.StockBalance;
import com.koda.platform.platform.stock.application.StockMovement;
import com.koda.platform.platform.stock.application.StockProduct;
import com.koda.platform.platform.stock.application.StockRepository;
import com.koda.platform.platform.stock.application.StockRequestMetadata;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.stock.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcStockRepository implements StockRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcStockRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<StockBalance> listBalances(TenantId tenantId, UUID warehouseId, UUID productId, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(balanceSelect()).append(" WHERE tenant_id = ?");
        args.add(tenantId.value());
        appendUuidFilter(sql, args, "warehouse_id", warehouseId);
        appendUuidFilter(sql, args, "product_id", productId);
        sql.append(" ORDER BY updated_at DESC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapBalance, args.toArray());
    }

    @Override
    public List<StockMovement> listMovements(TenantId tenantId, UUID warehouseId, UUID productId, int limit) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder(movementSelect()).append(" WHERE tenant_id = ?");
        args.add(tenantId.value());
        appendUuidFilter(sql, args, "warehouse_id", warehouseId);
        appendUuidFilter(sql, args, "product_id", productId);
        sql.append(" ORDER BY confirmed_at DESC, id DESC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), this::mapMovement, args.toArray());
    }

    @Override
    public Optional<StockMovement> findMovementById(TenantId tenantId, UUID id) {
        String sql = movementSelect() + " WHERE tenant_id = ? AND id = ?";
        return jdbcTemplate.query(sql, this::mapMovement, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Optional<StockProduct> findProductForStock(TenantId tenantId, UUID id) {
        String sql = """
            SELECT id, product_type, status, stock_tracking_enabled, allow_negative_stock
            FROM products
            WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, this::mapProduct, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public boolean existsActiveWarehouse(TenantId tenantId, UUID id) {
        String sql = """
            SELECT count(*)
            FROM warehouses
            WHERE tenant_id = ? AND id = ? AND is_active = true AND deleted_at IS NULL
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId.value(), id);
        return count != null && count > 0;
    }

    @Override
    public StockBalance lockOrCreateBalance(TenantId tenantId, UUID warehouseId, UUID productId) {
        String insertSql = """
            INSERT INTO stock_balances (tenant_id, warehouse_id, product_id)
            VALUES (?, ?, ?)
            ON CONFLICT (tenant_id, warehouse_id, product_id) DO NOTHING
            """;
        jdbcTemplate.update(insertSql, tenantId.value(), warehouseId, productId);

        String selectSql = balanceSelect() + " WHERE tenant_id = ? AND warehouse_id = ? AND product_id = ? FOR UPDATE";
        return jdbcTemplate.query(selectSql, this::mapBalance, tenantId.value(), warehouseId, productId).getFirst();
    }

    @Override
    public StockBalance updateBalanceQuantity(TenantId tenantId, UUID balanceId, BigDecimal quantityOnHand) {
        String sql = """
            UPDATE stock_balances
            SET quantity_on_hand = ?, updated_at = now(), version = version + 1
            WHERE tenant_id = ? AND id = ?
            RETURNING id, tenant_id, warehouse_id, product_id, quantity_on_hand, reserved_quantity, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapBalance, quantityOnHand, tenantId.value(), balanceId).getFirst();
    }

    @Override
    public StockMovement createMovement(TenantId tenantId, UUID actorUserId, CreateStockMovementCommand command,
                                        BigDecimal quantityBefore, BigDecimal quantityAfter, BigDecimal quantityDelta) {
        String sql = """
            INSERT INTO stock_movements (
                tenant_id, warehouse_id, product_id, movement_type, quantity, quantity_before, quantity_after, quantity_delta,
                unit_cost, currency_code, reference_type, reference_id, reason, confirmed_by, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, tenant_id, warehouse_id, product_id, movement_type, quantity, quantity_before, quantity_after, quantity_delta,
                unit_cost, currency_code, reference_type, reference_id, reversal_of_movement_id, reason, confirmed_at, confirmed_by
            """;
        return jdbcTemplate.query(sql, this::mapMovement,
            tenantId.value(), command.warehouseId(), command.productId(), command.movementType(), command.quantity(), quantityBefore, quantityAfter, quantityDelta,
            command.unitCost(), command.currencyCode(), command.referenceType(), command.referenceId(), command.reason(), actorUserId, actorUserId
        ).getFirst();
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                 String outcome, StockRequestMetadata metadata, Map<String, Object> details) {
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

    private void appendUuidFilter(StringBuilder sql, List<Object> args, String column, UUID value) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = ?");
            args.add(value);
        }
    }

    private String balanceSelect() {
        return """
            SELECT id, tenant_id, warehouse_id, product_id, quantity_on_hand, reserved_quantity, version, updated_at
            FROM stock_balances
            """;
    }

    private String movementSelect() {
        return """
            SELECT id, tenant_id, warehouse_id, product_id, movement_type, quantity, quantity_before, quantity_after, quantity_delta,
                   unit_cost, currency_code, reference_type, reference_id, reversal_of_movement_id, reason, confirmed_at, confirmed_by
            FROM stock_movements
            """;
    }

    private StockBalance mapBalance(ResultSet rs, int rowNum) throws SQLException {
        return new StockBalance(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("warehouse_id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getBigDecimal("quantity_on_hand"),
            rs.getBigDecimal("reserved_quantity"),
            rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private StockMovement mapMovement(ResultSet rs, int rowNum) throws SQLException {
        return new StockMovement(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("warehouse_id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getString("movement_type"),
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("quantity_before"),
            rs.getBigDecimal("quantity_after"),
            rs.getBigDecimal("quantity_delta"),
            rs.getBigDecimal("unit_cost"),
            rs.getString("currency_code"),
            rs.getString("reference_type"),
            rs.getObject("reference_id", UUID.class),
            rs.getObject("reversal_of_movement_id", UUID.class),
            rs.getString("reason"),
            rs.getTimestamp("confirmed_at").toInstant(),
            rs.getObject("confirmed_by", UUID.class)
        );
    }

    private StockProduct mapProduct(ResultSet rs, int rowNum) throws SQLException {
        return new StockProduct(
            rs.getObject("id", UUID.class),
            rs.getString("product_type"),
            rs.getString("status"),
            rs.getBoolean("stock_tracking_enabled"),
            rs.getBoolean("allow_negative_stock")
        );
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }
}