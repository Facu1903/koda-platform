package com.koda.platform.platform.licensing.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.licensing.application.LicenseAdministrationRequestMetadata;
import com.koda.platform.platform.licensing.application.TenantFeatureFlagAdministration;
import com.koda.platform.platform.licensing.application.TenantLicenseAdministration;
import com.koda.platform.platform.licensing.application.TenantLicenseAdministrationRepository;
import com.koda.platform.platform.licensing.application.TenantLicenseTenant;
import com.koda.platform.platform.licensing.application.TenantLimitOverrideAdministration;
import com.koda.platform.platform.licensing.application.TenantModuleEntitlementAdministration;
import com.koda.platform.platform.licensing.application.TenantProductEntitlementAdministration;
import com.koda.platform.platform.licensing.application.TenantProductSubscriptionAdministration;
import com.koda.platform.platform.licensing.application.UpdateTenantModuleEntitlementCommand;
import com.koda.platform.platform.licensing.application.UpdateTenantProductEntitlementCommand;
import com.koda.platform.platform.licensing.application.UpdateTenantProductSubscriptionCommand;
import com.koda.platform.shared.domain.tenant.TenantId;
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
@ConditionalOnProperty(prefix = "koda.licensing.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcTenantLicenseAdministrationRepository implements TenantLicenseAdministrationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcTenantLicenseAdministrationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TenantLicenseTenant> findTenant(TenantId tenantId) {
        String sql = """
            SELECT id, commercial_name, legal_name, status, country_code, default_locale, default_currency, time_zone, version
            FROM tenants
            WHERE id = ? AND deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, this::mapTenant, tenantId.value()).stream().findFirst();
    }

    @Override
    public TenantLicenseAdministration findAdministration(TenantLicenseTenant tenant) {
        return new TenantLicenseAdministration(
            tenant,
            Instant.now(),
            findSubscriptions(tenant.id()),
            findProductEntitlements(tenant.id()),
            findModuleEntitlements(tenant.id()),
            findLimitOverrides(tenant.id()),
            findFeatureFlags(tenant.id())
        );
    }

    @Override
    public Optional<TenantProductSubscriptionAdministration> findSubscription(TenantId tenantId, UUID subscriptionId) {
        return jdbcTemplate.query(subscriptionSelect() + " WHERE s.tenant_id = ? AND s.id = ?", this::mapSubscription, tenantId.value(), subscriptionId)
            .stream().findFirst();
    }

    @Override
    public Optional<TenantProductSubscriptionAdministration> updateSubscription(
        TenantId tenantId,
        UUID subscriptionId,
        UUID actorUserId,
        UpdateTenantProductSubscriptionCommand command
    ) {
        String sql = """
            WITH updated AS (
                UPDATE tenant_product_subscriptions
                SET status = ?,
                    valid_until = ?,
                    cancelled_at = CASE WHEN ? = 'CANCELLED' THEN COALESCE(cancelled_at, now()) ELSE NULL END,
                    updated_at = now(),
                    updated_by = ?,
                    version = version + 1
                WHERE tenant_id = ?
                  AND id = ?
                  AND version = ?
                RETURNING *
            )
            SELECT s.id, s.product_id, p.code AS product_code, p.name AS product_name,
                   s.plan_id, plan.code AS plan_code, plan.name AS plan_name,
                   s.status, s.valid_from, s.valid_until, s.source, s.cancelled_at, s.version, s.updated_at
            FROM updated s
            JOIN platform_products p ON p.id = s.product_id
            JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id
            """;
        return jdbcTemplate.query(sql, this::mapSubscription, command.status(), timestamp(command.validUntil()), command.status(), actorUserId,
            tenantId.value(), subscriptionId, command.version()).stream().findFirst();
    }

    @Override
    public Optional<TenantProductEntitlementAdministration> findProductEntitlement(TenantId tenantId, UUID entitlementId) {
        return jdbcTemplate.query(productEntitlementSelect() + " WHERE pe.tenant_id = ? AND pe.id = ?", this::mapProductEntitlement,
            tenantId.value(), entitlementId).stream().findFirst();
    }

    @Override
    public Optional<TenantProductEntitlementAdministration> updateProductEntitlement(
        TenantId tenantId,
        UUID entitlementId,
        UUID actorUserId,
        UpdateTenantProductEntitlementCommand command
    ) {
        String sql = """
            WITH updated AS (
                UPDATE tenant_product_entitlements
                SET status = ?,
                    valid_until = ?,
                    updated_at = now(),
                    updated_by = ?,
                    version = version + 1
                WHERE tenant_id = ?
                  AND id = ?
                  AND version = ?
                RETURNING *
            )
            SELECT pe.id, pe.product_id, p.code AS product_code, p.name AS product_name,
                   pe.status, pe.valid_from, pe.valid_until, pe.version, pe.updated_at
            FROM updated pe
            JOIN platform_products p ON p.id = pe.product_id
            """;
        return jdbcTemplate.query(sql, this::mapProductEntitlement, command.status(), timestamp(command.validUntil()), actorUserId,
            tenantId.value(), entitlementId, command.version()).stream().findFirst();
    }

    @Override
    public Optional<TenantModuleEntitlementAdministration> findModuleEntitlement(TenantId tenantId, UUID entitlementId) {
        return jdbcTemplate.query(moduleEntitlementSelect() + " WHERE me.tenant_id = ? AND me.id = ?", this::mapModuleEntitlement,
            tenantId.value(), entitlementId).stream().findFirst();
    }

    @Override
    public Optional<TenantModuleEntitlementAdministration> updateModuleEntitlement(
        TenantId tenantId,
        UUID entitlementId,
        UUID actorUserId,
        UpdateTenantModuleEntitlementCommand command
    ) {
        String sql = """
            WITH updated AS (
                UPDATE tenant_module_entitlements
                SET status = ?,
                    valid_until = ?,
                    updated_at = now(),
                    updated_by = ?,
                    version = version + 1
                WHERE tenant_id = ?
                  AND id = ?
                  AND version = ?
                RETURNING *
            )
            SELECT me.id, m.product_id, p.code AS product_code, p.name AS product_name,
                   me.module_id, m.code AS module_code, m.name AS module_name,
                   m.core_module, m.commercially_toggleable,
                   me.status, me.valid_from, me.valid_until, me.version, me.updated_at
            FROM updated me
            JOIN platform_modules m ON m.id = me.module_id
            JOIN platform_products p ON p.id = m.product_id
            """;
        return jdbcTemplate.query(sql, this::mapModuleEntitlement, command.status(), timestamp(command.validUntil()), actorUserId,
            tenantId.value(), entitlementId, command.version()).stream().findFirst();
    }

    @Override
    public void recordAuditEvent(
        TenantId tenantId,
        UUID actorUserId,
        String action,
        String resourceType,
        UUID resourceId,
        LicenseAdministrationRequestMetadata metadata,
        Map<String, Object> details
    ) {
        String sql = """
            INSERT INTO audit_events (
                tenant_id, actor_user_id, actor_type, action, resource_type, resource_id, outcome, source_ip, user_agent, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, 'SUCCESS', CAST(? AS inet), ?, CAST(? AS jsonb))
            """;
        jdbcTemplate.update(sql,
            tenantId == null ? null : tenantId.value(),
            actorUserId,
            actorUserId == null ? "SYSTEM" : "USER",
            action,
            resourceType,
            resourceId,
            metadata == null ? null : metadata.sourceIp(),
            metadata == null ? null : metadata.userAgent(),
            toJson(details)
        );
    }

    private List<TenantProductSubscriptionAdministration> findSubscriptions(TenantId tenantId) {
        return jdbcTemplate.query(subscriptionSelect() + " WHERE s.tenant_id = ? ORDER BY p.code, s.created_at DESC", this::mapSubscription,
            tenantId.value());
    }

    private List<TenantProductEntitlementAdministration> findProductEntitlements(TenantId tenantId) {
        return jdbcTemplate.query(productEntitlementSelect() + " WHERE pe.tenant_id = ? ORDER BY p.code", this::mapProductEntitlement,
            tenantId.value());
    }

    private List<TenantModuleEntitlementAdministration> findModuleEntitlements(TenantId tenantId) {
        return jdbcTemplate.query(moduleEntitlementSelect() + " WHERE me.tenant_id = ? ORDER BY p.code, m.code", this::mapModuleEntitlement,
            tenantId.value());
    }

    private List<TenantLimitOverrideAdministration> findLimitOverrides(TenantId tenantId) {
        String sql = """
            SELECT lo.id, lo.product_id, p.code AS product_code, p.name AS product_name, lo.code, lo.limit_value, lo.unlimited, lo.unit, lo.reason,
                   lo.valid_from, lo.valid_until, lo.version, lo.updated_at
            FROM tenant_limit_overrides lo
            JOIN platform_products p ON p.id = lo.product_id
            WHERE lo.tenant_id = ?
            ORDER BY p.code, lo.code
            """;
        return jdbcTemplate.query(sql, this::mapLimitOverride, tenantId.value());
    }

    private List<TenantFeatureFlagAdministration> findFeatureFlags(TenantId tenantId) {
        String sql = """
            SELECT f.id, f.product_id, p.code AS product_code, p.name AS product_name, f.module_id, m.code AS module_code, f.code, f.enabled, f.reason,
                   f.valid_from, f.valid_until, f.version, f.updated_at
            FROM tenant_feature_flags f
            JOIN platform_products p ON p.id = f.product_id
            LEFT JOIN platform_modules m ON m.id = f.module_id
            WHERE f.tenant_id = ?
            ORDER BY p.code, m.code NULLS FIRST, f.code
            """;
        return jdbcTemplate.query(sql, this::mapFeatureFlag, tenantId.value());
    }

    private String subscriptionSelect() {
        return """
            SELECT s.id, s.product_id, p.code AS product_code, p.name AS product_name,
                   s.plan_id, plan.code AS plan_code, plan.name AS plan_name,
                   s.status, s.valid_from, s.valid_until, s.source, s.cancelled_at, s.version, s.updated_at
            FROM tenant_product_subscriptions s
            JOIN platform_products p ON p.id = s.product_id
            JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id
            """;
    }

    private String productEntitlementSelect() {
        return """
            SELECT pe.id, pe.product_id, p.code AS product_code, p.name AS product_name,
                   pe.status, pe.valid_from, pe.valid_until, pe.version, pe.updated_at
            FROM tenant_product_entitlements pe
            JOIN platform_products p ON p.id = pe.product_id
            """;
    }

    private String moduleEntitlementSelect() {
        return """
            SELECT me.id, m.product_id, p.code AS product_code, p.name AS product_name,
                   me.module_id, m.code AS module_code, m.name AS module_name,
                   m.core_module, m.commercially_toggleable,
                   me.status, me.valid_from, me.valid_until, me.version, me.updated_at
            FROM tenant_module_entitlements me
            JOIN platform_modules m ON m.id = me.module_id
            JOIN platform_products p ON p.id = m.product_id
            """;
    }

    private TenantLicenseTenant mapTenant(ResultSet rs, int rowNum) throws SQLException {
        return new TenantLicenseTenant(
            TenantId.from(rs.getObject("id", UUID.class)),
            rs.getString("commercial_name"),
            rs.getString("legal_name"),
            rs.getString("status"),
            rs.getString("country_code"),
            rs.getString("default_locale"),
            rs.getString("default_currency"),
            rs.getString("time_zone"),
            rs.getLong("version")
        );
    }

    private TenantProductSubscriptionAdministration mapSubscription(ResultSet rs, int rowNum) throws SQLException {
        return new TenantProductSubscriptionAdministration(
            rs.getObject("id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getObject("plan_id", UUID.class),
            rs.getString("plan_code"),
            rs.getString("plan_name"),
            rs.getString("status"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until"),
            rs.getString("source"),
            instant(rs, "cancelled_at"),
            rs.getLong("version"),
            instant(rs, "updated_at")
        );
    }

    private TenantProductEntitlementAdministration mapProductEntitlement(ResultSet rs, int rowNum) throws SQLException {
        return new TenantProductEntitlementAdministration(
            rs.getObject("id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getString("status"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until"),
            rs.getLong("version"),
            instant(rs, "updated_at")
        );
    }

    private TenantModuleEntitlementAdministration mapModuleEntitlement(ResultSet rs, int rowNum) throws SQLException {
        return new TenantModuleEntitlementAdministration(
            rs.getObject("id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getObject("module_id", UUID.class),
            rs.getString("module_code"),
            rs.getString("module_name"),
            rs.getBoolean("core_module"),
            rs.getBoolean("commercially_toggleable"),
            rs.getString("status"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until"),
            rs.getLong("version"),
            instant(rs, "updated_at")
        );
    }

    private TenantLimitOverrideAdministration mapLimitOverride(ResultSet rs, int rowNum) throws SQLException {
        long limitValue = rs.getLong("limit_value");
        return new TenantLimitOverrideAdministration(
            rs.getObject("id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getString("code"),
            rs.wasNull() ? null : limitValue,
            rs.getBoolean("unlimited"),
            rs.getString("unit"),
            rs.getString("reason"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until"),
            rs.getLong("version"),
            instant(rs, "updated_at")
        );
    }

    private TenantFeatureFlagAdministration mapFeatureFlag(ResultSet rs, int rowNum) throws SQLException {
        return new TenantFeatureFlagAdministration(
            rs.getObject("id", UUID.class),
            rs.getObject("product_id", UUID.class),
            rs.getString("product_code"),
            rs.getString("product_name"),
            rs.getObject("module_id", UUID.class),
            rs.getString("module_code"),
            rs.getString("code"),
            rs.getBoolean("enabled"),
            rs.getString("reason"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until"),
            rs.getLong("version"),
            instant(rs, "updated_at")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }
}
