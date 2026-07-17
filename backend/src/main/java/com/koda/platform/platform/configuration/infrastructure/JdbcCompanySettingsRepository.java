package com.koda.platform.platform.configuration.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.configuration.application.ClientRequestMetadata;
import com.koda.platform.platform.configuration.application.CompanySettings;
import com.koda.platform.platform.configuration.application.CompanySettingsRepository;
import com.koda.platform.platform.configuration.application.UpdateCompanySettingsCommand;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.configuration.company-settings.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcCompanySettingsRepository implements CompanySettingsRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCompanySettingsRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CompanySettings> findByTenantId(TenantId tenantId) {
        String sql = selectSql() + " WHERE t.id = ? AND t.deleted_at IS NULL";
        return jdbcTemplate.query(sql, this::mapCompanySettings, tenantId.value()).stream().findFirst();
    }

    @Override
    public Optional<CompanySettings> update(TenantId tenantId, UUID actorUserId, UpdateCompanySettingsCommand command) {
        String updateTenantSql = """
            UPDATE tenants
            SET default_locale = ?, default_currency = ?, time_zone = ?, updated_at = now(), updated_by = ?, version = version + 1
            WHERE id = ? AND deleted_at IS NULL
            """;
        jdbcTemplate.update(updateTenantSql,
            command.defaultLocale(),
            command.defaultCurrency(),
            command.timeZone(),
            actorUserId,
            tenantId.value()
        );

        String updateSettingsSql = """
            UPDATE company_settings
            SET logo_url = ?,
                favicon_url = ?,
                login_image_url = ?,
                primary_color = ?,
                secondary_color = ?,
                theme_mode = ?,
                date_format = ?,
                time_format = ?,
                number_locale = ?,
                currency_format = ?,
                updated_at = now(),
                updated_by = ?,
                version = version + 1
            WHERE tenant_id = ? AND version = ?
            RETURNING id
            """;
        List<UUID> updatedIds = jdbcTemplate.query(updateSettingsSql,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            command.logoUrl(),
            command.faviconUrl(),
            command.loginImageUrl(),
            command.primaryColor(),
            command.secondaryColor(),
            command.themeMode(),
            command.dateFormat(),
            command.timeFormat(),
            command.numberLocale(),
            command.currencyFormat(),
            actorUserId,
            tenantId.value(),
            command.version()
        );
        if (updatedIds.isEmpty()) {
            return Optional.empty();
        }
        return findByTenantId(tenantId);
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String outcome, UUID resourceId,
                                 ClientRequestMetadata metadata, Map<String, Object> details) {
        String sql = """
            INSERT INTO audit_events (
                tenant_id, actor_user_id, actor_type, action, resource_type, resource_id, outcome, source_ip, user_agent, metadata
            ) VALUES (?, ?, ?, ?, 'company_settings', ?, ?, CAST(? AS inet), ?, CAST(? AS jsonb))
            """;
        jdbcTemplate.update(sql,
            tenantId == null ? null : tenantId.value(),
            actorUserId,
            actorUserId == null ? "SYSTEM" : "USER",
            action,
            resourceId,
            outcome,
            metadata == null ? null : metadata.sourceIp(),
            metadata == null ? null : metadata.userAgent(),
            toJson(details)
        );
    }

    private String selectSql() {
        return """
            SELECT
                cs.id,
                t.id AS tenant_id,
                t.commercial_name,
                t.legal_name,
                t.country_code,
                t.default_locale,
                t.default_currency,
                t.time_zone,
                cs.logo_url,
                cs.favicon_url,
                cs.login_image_url,
                cs.primary_color,
                cs.secondary_color,
                cs.theme_mode,
                cs.date_format,
                cs.time_format,
                cs.number_locale,
                cs.currency_format,
                cs.version,
                cs.updated_at
            FROM company_settings cs
            JOIN tenants t ON t.id = cs.tenant_id
            """;
    }

    private CompanySettings mapCompanySettings(ResultSet rs, int rowNum) throws SQLException {
        return new CompanySettings(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getString("commercial_name"),
            rs.getString("legal_name"),
            rs.getString("country_code"),
            rs.getString("default_locale"),
            rs.getString("default_currency"),
            rs.getString("time_zone"),
            rs.getString("logo_url"),
            rs.getString("favicon_url"),
            rs.getString("login_image_url"),
            rs.getString("primary_color"),
            rs.getString("secondary_color"),
            rs.getString("theme_mode"),
            rs.getString("date_format"),
            rs.getString("time_format"),
            rs.getString("number_locale"),
            rs.getString("currency_format"),
            rs.getLong("version"),
            rs.getTimestamp("updated_at").toInstant()
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