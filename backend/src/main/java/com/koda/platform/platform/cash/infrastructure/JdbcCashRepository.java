package com.koda.platform.platform.cash.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.cash.application.CashMovement;
import com.koda.platform.platform.cash.application.CashRegister;
import com.koda.platform.platform.cash.application.CashRepository;
import com.koda.platform.platform.cash.application.CashRequestMetadata;
import com.koda.platform.platform.cash.application.CashSession;
import com.koda.platform.platform.cash.application.CloseCashSessionCommand;
import com.koda.platform.platform.cash.application.CreateCashMovementCommand;
import com.koda.platform.platform.cash.application.OpenCashSessionCommand;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.cash.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcCashRepository implements CashRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCashRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CashRegister> listRegisters(TenantId tenantId) {
        return jdbcTemplate.query(registerSelect() + " WHERE tenant_id = ? AND deleted_at IS NULL ORDER BY name", this::mapRegister, tenantId.value());
    }

    @Override
    public Optional<CashRegister> findActiveRegister(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(registerSelect() + " WHERE tenant_id = ? AND id = ? AND status = 'ACTIVE' AND deleted_at IS NULL", this::mapRegister,
            tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Optional<String> findTenantCurrency(TenantId tenantId) {
        String sql = "SELECT default_currency FROM tenants WHERE id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("default_currency"), tenantId.value()).stream().findFirst();
    }

    @Override
    public List<CashSession> listSessions(TenantId tenantId, UUID openedByUserId, int limit) {
        if (openedByUserId == null) {
            return jdbcTemplate.query(sessionSelect() + " WHERE tenant_id = ? ORDER BY opened_at DESC LIMIT ?", this::mapSession, tenantId.value(), limit);
        }
        return jdbcTemplate.query(sessionSelect() + " WHERE tenant_id = ? AND opened_by_user_id = ? ORDER BY opened_at DESC LIMIT ?", this::mapSession,
            tenantId.value(), openedByUserId, limit);
    }

    @Override
    public Optional<CashSession> findSessionById(TenantId tenantId, UUID id) {
        return jdbcTemplate.query(sessionSelect() + " WHERE tenant_id = ? AND id = ?", this::mapSession, tenantId.value(), id).stream().findFirst();
    }

    @Override
    public Optional<CashSession> findCurrentOpenSession(TenantId tenantId, UUID openedByUserId) {
        String sql = sessionSelect() + " WHERE tenant_id = ? AND opened_by_user_id = ? AND status = 'OPEN' ORDER BY opened_at DESC LIMIT 1";
        return jdbcTemplate.query(sql, this::mapSession, tenantId.value(), openedByUserId).stream().findFirst();
    }

    @Override
    public boolean existsOpenSession(TenantId tenantId, UUID cashRegisterId, UUID openedByUserId) {
        String sql = """
            SELECT count(*)
            FROM cash_sessions
            WHERE tenant_id = ?
              AND cash_register_id = ?
              AND opened_by_user_id = ?
              AND status = 'OPEN'
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId.value(), cashRegisterId, openedByUserId);
        return count != null && count > 0;
    }

    @Override
    public CashSession createSession(TenantId tenantId, UUID actorUserId, CashRegister cashRegister, OpenCashSessionCommand command) {
        String sql = """
            INSERT INTO cash_sessions (
                tenant_id, cash_register_id, branch_id, opened_by_user_id, status, opening_amount, currency_code, created_by, updated_by
            ) VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?, ?)
            RETURNING id, tenant_id, cash_register_id, branch_id, opened_by_user_id, status, opening_amount, currency_code,
                expected_closing_amount, counted_closing_amount, closing_difference, opened_at, closed_at, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapSession, tenantId.value(), cashRegister.id(), cashRegister.branchId(), actorUserId,
            command.openingAmount(), command.currencyCode(), actorUserId, actorUserId).getFirst();
    }

    @Override
    public Optional<CashSession> closeSession(TenantId tenantId, UUID id, UUID actorUserId, CloseCashSessionCommand command,
                                              BigDecimal expectedClosingAmount, BigDecimal closingDifference) {
        String sql = """
            UPDATE cash_sessions
            SET status = 'CLOSED', expected_closing_amount = ?, counted_closing_amount = ?, closing_difference = ?,
                closed_at = now(), closed_by_user_id = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND status = 'OPEN'
            RETURNING id, tenant_id, cash_register_id, branch_id, opened_by_user_id, status, opening_amount, currency_code,
                expected_closing_amount, counted_closing_amount, closing_difference, opened_at, closed_at, version, updated_at
            """;
        return jdbcTemplate.query(sql, this::mapSession, expectedClosingAmount, command.countedClosingAmount(), closingDifference, actorUserId,
            actorUserId, tenantId.value(), id, command.version()).stream().findFirst();
    }

    @Override
    public List<CashMovement> listMovements(TenantId tenantId, UUID cashSessionId, int limit) {
        String sql = movementSelect() + " WHERE tenant_id = ? AND cash_session_id = ? ORDER BY occurred_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, this::mapMovement, tenantId.value(), cashSessionId, limit);
    }

    @Override
    public CashMovement createMovement(TenantId tenantId, UUID actorUserId, CashSession cashSession, CreateCashMovementCommand command,
                                       BigDecimal cashEffect) {
        String sql = """
            INSERT INTO cash_movements (
                tenant_id, cash_session_id, cash_register_id, branch_id, movement_type, payment_method, amount, cash_effect,
                currency_code, reference_type, reference_id, description, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, tenant_id, cash_session_id, cash_register_id, branch_id, movement_type, payment_method, amount, cash_effect,
                currency_code, reference_type, reference_id, description, created_by, occurred_at
            """;
        return jdbcTemplate.query(sql, this::mapMovement, tenantId.value(), cashSession.id(), cashSession.cashRegisterId(), cashSession.branchId(),
            command.movementType(), command.paymentMethod(), command.amount(), cashEffect, command.currencyCode(), command.referenceType(),
            command.referenceId(), command.description(), actorUserId).getFirst();
    }

    @Override
    public BigDecimal sumCashEffect(TenantId tenantId, UUID cashSessionId, String currencyCode) {
        String sql = """
            SELECT COALESCE(SUM(cash_effect), 0)
            FROM cash_movements
            WHERE tenant_id = ?
              AND cash_session_id = ?
              AND currency_code = ?
            """;
        BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class, tenantId.value(), cashSessionId, currencyCode);
        return (total == null ? BigDecimal.ZERO : total).setScale(4, RoundingMode.UNNECESSARY);
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                 String outcome, CashRequestMetadata metadata, Map<String, Object> details) {
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

    private String registerSelect() {
        return "SELECT id, tenant_id, branch_id, code, name, status, version, updated_at FROM cash_registers";
    }

    private String sessionSelect() {
        return """
            SELECT id, tenant_id, cash_register_id, branch_id, opened_by_user_id, status, opening_amount, currency_code,
                   expected_closing_amount, counted_closing_amount, closing_difference, opened_at, closed_at, version, updated_at
            FROM cash_sessions
            """;
    }

    private String movementSelect() {
        return """
            SELECT id, tenant_id, cash_session_id, cash_register_id, branch_id, movement_type, payment_method, amount, cash_effect,
                   currency_code, reference_type, reference_id, description, created_by, occurred_at
            FROM cash_movements
            """;
    }

    private CashRegister mapRegister(ResultSet rs, int rowNum) throws SQLException {
        return new CashRegister(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("branch_id", UUID.class),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("status"),
            rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private CashSession mapSession(ResultSet rs, int rowNum) throws SQLException {
        Timestamp closedAt = rs.getTimestamp("closed_at");
        return new CashSession(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("cash_register_id", UUID.class),
            rs.getObject("branch_id", UUID.class),
            rs.getObject("opened_by_user_id", UUID.class),
            rs.getString("status"),
            rs.getBigDecimal("opening_amount"),
            rs.getString("currency_code"),
            rs.getBigDecimal("expected_closing_amount"),
            rs.getBigDecimal("counted_closing_amount"),
            rs.getBigDecimal("closing_difference"),
            rs.getTimestamp("opened_at").toInstant(),
            closedAt == null ? null : closedAt.toInstant(),
            rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }

    private CashMovement mapMovement(ResultSet rs, int rowNum) throws SQLException {
        return new CashMovement(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("cash_session_id", UUID.class),
            rs.getObject("cash_register_id", UUID.class),
            rs.getObject("branch_id", UUID.class),
            rs.getString("movement_type"),
            rs.getString("payment_method"),
            rs.getBigDecimal("amount"),
            rs.getBigDecimal("cash_effect"),
            rs.getString("currency_code"),
            rs.getString("reference_type"),
            rs.getObject("reference_id", UUID.class),
            rs.getString("description"),
            rs.getObject("created_by", UUID.class),
            rs.getTimestamp("occurred_at").toInstant()
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