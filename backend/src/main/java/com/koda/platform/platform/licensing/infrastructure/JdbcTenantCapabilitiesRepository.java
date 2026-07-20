package com.koda.platform.platform.licensing.infrastructure;

import com.koda.platform.platform.licensing.application.FeatureFlagCapability;
import com.koda.platform.platform.licensing.application.LimitCapability;
import com.koda.platform.platform.licensing.application.ModuleCapability;
import com.koda.platform.platform.licensing.application.ProductCapability;
import com.koda.platform.platform.licensing.application.TenantCapabilitiesRepository;
import com.koda.platform.platform.licensing.application.TenantCapabilityTenant;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessRepository;
import com.koda.platform.shared.domain.tenant.TenantId;
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
@ConditionalOnProperty(prefix = "koda.licensing.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcTenantCapabilitiesRepository implements TenantCapabilitiesRepository, TenantLicenseAccessRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantCapabilitiesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isProductEnabled(TenantId tenantId, String productCode, Instant calculatedAt) {
        Timestamp now = timestamp(calculatedAt);
        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM tenant_product_subscriptions s
                JOIN tenants t ON t.id = s.tenant_id AND t.status = 'ACTIVE' AND t.deleted_at IS NULL
                JOIN platform_products p ON p.id = s.product_id AND p.status = 'ACTIVE'
                JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id AND plan.status = 'ACTIVE'
                JOIN tenant_product_entitlements pe ON pe.tenant_id = s.tenant_id AND pe.product_id = s.product_id
                WHERE s.tenant_id = ?
                  AND p.code = ?
                  AND s.status = 'ACTIVE'
                  AND s.valid_from <= ?
                  AND (s.valid_until IS NULL OR s.valid_until > ?)
                  AND pe.status = 'ACTIVE'
                  AND pe.valid_from <= ?
                  AND (pe.valid_until IS NULL OR pe.valid_until > ?)
            )
            """;
        Boolean enabled = jdbcTemplate.queryForObject(sql, Boolean.class, tenantId.value(), productCode, now, now, now, now);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public boolean isModuleEnabled(TenantId tenantId, String productCode, String moduleCode, Instant calculatedAt) {
        Timestamp now = timestamp(calculatedAt);
        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM tenant_product_subscriptions s
                JOIN tenants t ON t.id = s.tenant_id AND t.status = 'ACTIVE' AND t.deleted_at IS NULL
                JOIN platform_products p ON p.id = s.product_id AND p.status = 'ACTIVE'
                JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id AND plan.status = 'ACTIVE'
                JOIN tenant_product_entitlements pe ON pe.tenant_id = s.tenant_id AND pe.product_id = s.product_id
                JOIN product_plan_modules ppm ON ppm.plan_id = s.plan_id AND ppm.product_id = s.product_id
                JOIN platform_modules m ON m.id = ppm.module_id AND m.product_id = s.product_id AND m.status = 'ACTIVE'
                JOIN tenant_module_entitlements me ON me.tenant_id = s.tenant_id AND me.module_id = m.id
                WHERE s.tenant_id = ?
                  AND p.code = ?
                  AND m.code = ?
                  AND s.status = 'ACTIVE'
                  AND s.valid_from <= ?
                  AND (s.valid_until IS NULL OR s.valid_until > ?)
                  AND pe.status = 'ACTIVE'
                  AND pe.valid_from <= ?
                  AND (pe.valid_until IS NULL OR pe.valid_until > ?)
                  AND me.status = 'ACTIVE'
                  AND me.valid_from <= ?
                  AND (me.valid_until IS NULL OR me.valid_until > ?)
            )
            """;
        Boolean enabled = jdbcTemplate.queryForObject(sql, Boolean.class, tenantId.value(), productCode, moduleCode, now, now, now, now, now, now);
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public Optional<TenantCapabilityTenant> findTenant(TenantId tenantId) {
        String sql = "SELECT id, status FROM tenants WHERE id = ? AND deleted_at IS NULL";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TenantCapabilityTenant(TenantId.from(rs.getObject("id", UUID.class)), rs.getString("status")),
            tenantId.value()).stream().findFirst();
    }

    @Override
    public List<ProductCapability> findEnabledProducts(TenantId tenantId, Instant calculatedAt) {
        Timestamp now = timestamp(calculatedAt);
        String sql = """
            SELECT p.id, p.code, p.name,
                   pe.status AS entitlement_status,
                   pe.valid_from AS entitlement_valid_from,
                   pe.valid_until AS entitlement_valid_until,
                   s.id AS subscription_id,
                   s.status AS subscription_status,
                   s.valid_from AS subscription_valid_from,
                   s.valid_until AS subscription_valid_until,
                   plan.code AS plan_code,
                   plan.name AS plan_name
            FROM tenant_product_subscriptions s
            JOIN platform_products p ON p.id = s.product_id AND p.status = 'ACTIVE'
            JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id AND plan.status = 'ACTIVE'
            JOIN tenant_product_entitlements pe ON pe.tenant_id = s.tenant_id AND pe.product_id = s.product_id
            WHERE s.tenant_id = ?
              AND s.status = 'ACTIVE'
              AND s.valid_from <= ?
              AND (s.valid_until IS NULL OR s.valid_until > ?)
              AND pe.status = 'ACTIVE'
              AND pe.valid_from <= ?
              AND (pe.valid_until IS NULL OR pe.valid_until > ?)
            ORDER BY p.code
            """;
        return jdbcTemplate.query(sql, this::mapProductCapability, tenantId.value(), now, now, now, now);
    }

    @Override
    public List<ModuleCapability> findEnabledModules(TenantId tenantId, Instant calculatedAt) {
        Timestamp now = timestamp(calculatedAt);
        String sql = """
            SELECT m.id, p.code AS product_code, m.code, m.name, m.core_module, m.commercially_toggleable,
                   me.status AS entitlement_status,
                   me.valid_from,
                   me.valid_until
            FROM tenant_product_subscriptions s
            JOIN platform_products p ON p.id = s.product_id AND p.status = 'ACTIVE'
            JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id AND plan.status = 'ACTIVE'
            JOIN tenant_product_entitlements pe ON pe.tenant_id = s.tenant_id AND pe.product_id = s.product_id
            JOIN product_plan_modules ppm ON ppm.plan_id = s.plan_id AND ppm.product_id = s.product_id
            JOIN platform_modules m ON m.id = ppm.module_id AND m.product_id = s.product_id AND m.status = 'ACTIVE'
            JOIN tenant_module_entitlements me ON me.tenant_id = s.tenant_id AND me.module_id = m.id
            WHERE s.tenant_id = ?
              AND s.status = 'ACTIVE'
              AND s.valid_from <= ?
              AND (s.valid_until IS NULL OR s.valid_until > ?)
              AND pe.status = 'ACTIVE'
              AND pe.valid_from <= ?
              AND (pe.valid_until IS NULL OR pe.valid_until > ?)
              AND me.status = 'ACTIVE'
              AND me.valid_from <= ?
              AND (me.valid_until IS NULL OR me.valid_until > ?)
            ORDER BY p.code, m.code
            """;
        return jdbcTemplate.query(sql, this::mapModuleCapability, tenantId.value(), now, now, now, now, now, now);
    }

    @Override
    public List<FeatureFlagCapability> findEffectiveFeatureFlags(TenantId tenantId, Instant calculatedAt) {
        Timestamp now = timestamp(calculatedAt);
        String sql = """
            SELECT p.code AS product_code,
                   m.code AS module_code,
                   f.code,
                   f.enabled,
                   f.valid_from,
                   f.valid_until
            FROM tenant_feature_flags f
            JOIN platform_products p ON p.id = f.product_id AND p.status = 'ACTIVE'
            LEFT JOIN platform_modules m ON m.id = f.module_id AND m.product_id = f.product_id AND m.status = 'ACTIVE'
            WHERE f.tenant_id = ?
              AND f.valid_from <= ?
              AND (f.valid_until IS NULL OR f.valid_until > ?)
              AND EXISTS (
                  SELECT 1
                  FROM tenant_product_subscriptions s
                  JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id AND plan.status = 'ACTIVE'
                  JOIN tenant_product_entitlements pe ON pe.tenant_id = s.tenant_id AND pe.product_id = s.product_id
                  WHERE s.tenant_id = f.tenant_id
                    AND s.product_id = f.product_id
                    AND s.status = 'ACTIVE'
                    AND s.valid_from <= ?
                    AND (s.valid_until IS NULL OR s.valid_until > ?)
                    AND pe.status = 'ACTIVE'
                    AND pe.valid_from <= ?
                    AND (pe.valid_until IS NULL OR pe.valid_until > ?)
              )
              AND (
                  f.module_id IS NULL
                  OR EXISTS (
                      SELECT 1
                      FROM tenant_product_subscriptions s
                      JOIN product_plan_modules ppm ON ppm.plan_id = s.plan_id AND ppm.product_id = s.product_id AND ppm.module_id = f.module_id
                      JOIN tenant_module_entitlements me ON me.tenant_id = s.tenant_id AND me.module_id = f.module_id
                      WHERE s.tenant_id = f.tenant_id
                        AND s.product_id = f.product_id
                        AND s.status = 'ACTIVE'
                        AND s.valid_from <= ?
                        AND (s.valid_until IS NULL OR s.valid_until > ?)
                        AND me.status = 'ACTIVE'
                        AND me.valid_from <= ?
                        AND (me.valid_until IS NULL OR me.valid_until > ?)
                  )
              )
            ORDER BY p.code, m.code NULLS FIRST, f.code
            """;
        return jdbcTemplate.query(sql, this::mapFeatureFlagCapability, tenantId.value(), now, now, now, now, now, now, now, now, now, now);
    }

    @Override
    public List<LimitCapability> findEffectiveLimits(TenantId tenantId, Instant calculatedAt) {
        Timestamp now = timestamp(calculatedAt);
        String sql = """
            SELECT p.code AS product_code,
                   plan_limit.code,
                   CASE WHEN tenant_limit.id IS NULL THEN plan_limit.limit_value ELSE tenant_limit.limit_value END AS limit_value,
                   CASE WHEN tenant_limit.id IS NULL THEN plan_limit.unlimited ELSE tenant_limit.unlimited END AS unlimited,
                   CASE WHEN tenant_limit.id IS NULL THEN plan_limit.unit ELSE tenant_limit.unit END AS unit,
                   CASE WHEN tenant_limit.id IS NULL THEN 'PLAN' ELSE 'TENANT_OVERRIDE' END AS source
            FROM tenant_product_subscriptions s
            JOIN platform_products p ON p.id = s.product_id AND p.status = 'ACTIVE'
            JOIN product_plans plan ON plan.id = s.plan_id AND plan.product_id = s.product_id AND plan.status = 'ACTIVE'
            JOIN tenant_product_entitlements pe ON pe.tenant_id = s.tenant_id AND pe.product_id = s.product_id
            JOIN product_plan_limits plan_limit ON plan_limit.plan_id = s.plan_id AND plan_limit.product_id = s.product_id
            LEFT JOIN tenant_limit_overrides tenant_limit ON tenant_limit.tenant_id = s.tenant_id
                AND tenant_limit.product_id = s.product_id
                AND tenant_limit.code = plan_limit.code
                AND tenant_limit.valid_from <= ?
                AND (tenant_limit.valid_until IS NULL OR tenant_limit.valid_until > ?)
            WHERE s.tenant_id = ?
              AND s.status = 'ACTIVE'
              AND s.valid_from <= ?
              AND (s.valid_until IS NULL OR s.valid_until > ?)
              AND pe.status = 'ACTIVE'
              AND pe.valid_from <= ?
              AND (pe.valid_until IS NULL OR pe.valid_until > ?)
            ORDER BY p.code, plan_limit.code
            """;
        return jdbcTemplate.query(sql, this::mapLimitCapability, now, now, tenantId.value(), now, now, now, now);
    }

    private ProductCapability mapProductCapability(ResultSet rs, int rowNum) throws SQLException {
        return new ProductCapability(
            rs.getObject("id", UUID.class),
            rs.getString("code"),
            rs.getString("name"),
            true,
            rs.getString("entitlement_status"),
            instant(rs, "entitlement_valid_from"),
            instant(rs, "entitlement_valid_until"),
            rs.getObject("subscription_id", UUID.class),
            rs.getString("subscription_status"),
            instant(rs, "subscription_valid_from"),
            instant(rs, "subscription_valid_until"),
            rs.getString("plan_code"),
            rs.getString("plan_name"),
            List.of()
        );
    }

    private ModuleCapability mapModuleCapability(ResultSet rs, int rowNum) throws SQLException {
        return new ModuleCapability(
            rs.getObject("id", UUID.class),
            rs.getString("product_code"),
            rs.getString("code"),
            rs.getString("name"),
            true,
            rs.getBoolean("core_module"),
            rs.getBoolean("commercially_toggleable"),
            rs.getString("entitlement_status"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until")
        );
    }

    private FeatureFlagCapability mapFeatureFlagCapability(ResultSet rs, int rowNum) throws SQLException {
        return new FeatureFlagCapability(
            rs.getString("product_code"),
            rs.getString("module_code"),
            rs.getString("code"),
            rs.getBoolean("enabled"),
            instant(rs, "valid_from"),
            instant(rs, "valid_until")
        );
    }

    private LimitCapability mapLimitCapability(ResultSet rs, int rowNum) throws SQLException {
        long value = rs.getLong("limit_value");
        Long nullableValue = rs.wasNull() ? null : value;
        return new LimitCapability(
            rs.getString("product_code"),
            rs.getString("code"),
            nullableValue,
            rs.getBoolean("unlimited"),
            rs.getString("unit"),
            rs.getString("source")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}