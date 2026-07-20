package com.koda.platform.platform.commercial.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.commercial.application.CommercialPartner;
import com.koda.platform.platform.commercial.application.CommercialPartnerRepository;
import com.koda.platform.platform.commercial.application.CommercialRequestMetadata;
import com.koda.platform.platform.commercial.application.CreateCommercialPartnerCommand;
import com.koda.platform.platform.commercial.application.UpdateCommercialPartnerCommand;
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
@ConditionalOnProperty(prefix = "koda.commercial.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcCommercialPartnerRepository implements CommercialPartnerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCommercialPartnerRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<CommercialPartner> listByRole(TenantId tenantId, String roleType, int limit) {
        String sql = partnerSelect() + """
            WHERE p.tenant_id = ?
              AND r.role_type = ?
              AND p.deleted_at IS NULL
              AND r.deleted_at IS NULL
            ORDER BY p.legal_name
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, this::mapPartner, tenantId.value(), roleType, limit);
    }

    @Override
    public Optional<CommercialPartner> findByIdAndRole(TenantId tenantId, UUID id, String roleType) {
        String sql = partnerSelect() + """
            WHERE p.tenant_id = ?
              AND p.id = ?
              AND r.role_type = ?
              AND p.deleted_at IS NULL
              AND r.deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, this::mapPartner, tenantId.value(), id, roleType).stream().findFirst();
    }

    @Override
    public Optional<UUID> findActivePartnerIdByDocument(TenantId tenantId, String documentType, String documentNumber) {
        String sql = """
            SELECT id
            FROM business_partners
            WHERE tenant_id = ?
              AND lower(document_type) = lower(?)
              AND lower(document_number) = lower(?)
              AND deleted_at IS NULL
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), tenantId.value(), documentType, documentNumber)
            .stream().findFirst();
    }

    @Override
    public boolean existsActiveRole(TenantId tenantId, UUID partnerId, String roleType) {
        String sql = """
            SELECT count(*)
            FROM business_partner_roles
            WHERE tenant_id = ?
              AND business_partner_id = ?
              AND role_type = ?
              AND deleted_at IS NULL
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tenantId.value(), partnerId, roleType);
        return count != null && count > 0;
    }

    @Override
    public CommercialPartner createPartnerWithRole(TenantId tenantId, UUID actorUserId, String roleType, boolean system,
                                                   CreateCommercialPartnerCommand command) {
        String sql = """
            INSERT INTO business_partners (
                tenant_id, legal_name, commercial_name, document_type, document_number, tax_condition, email, phone,
                address_line, city, province_code, country_code, notes, status, is_system, created_by, updated_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        UUID partnerId = jdbcTemplate.queryForObject(sql, UUID.class,
            tenantId.value(), command.legalName(), command.commercialName(), command.documentType(), command.documentNumber(), command.taxCondition(),
            command.email(), command.phone(), command.addressLine(), command.city(), command.provinceCode(), command.countryCode(), command.notes(),
            command.status(), system, actorUserId, actorUserId
        );
        insertRole(tenantId, partnerId, actorUserId, roleType, system);
        return findByIdAndRole(tenantId, partnerId, roleType).orElseThrow();
    }

    @Override
    public CommercialPartner addRoleToPartner(TenantId tenantId, UUID partnerId, UUID actorUserId, String roleType, boolean system) {
        String sql = """
            UPDATE business_partners
            SET updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND deleted_at IS NULL
            """;
        jdbcTemplate.update(sql, actorUserId, tenantId.value(), partnerId);
        insertRole(tenantId, partnerId, actorUserId, roleType, system);
        return findByIdAndRole(tenantId, partnerId, roleType).orElseThrow();
    }

    @Override
    public Optional<CommercialPartner> updatePartner(TenantId tenantId, UUID id, String roleType, UUID actorUserId,
                                                     UpdateCommercialPartnerCommand command) {
        String sql = """
            UPDATE business_partners p
            SET legal_name = ?, commercial_name = ?, document_type = ?, document_number = ?, tax_condition = ?, email = ?, phone = ?,
                address_line = ?, city = ?, province_code = ?, country_code = ?, notes = ?, status = ?, updated_at = now(),
                updated_by = ?, version = version + 1
            WHERE p.tenant_id = ?
              AND p.id = ?
              AND p.version = ?
              AND p.deleted_at IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM business_partner_roles r
                  WHERE r.tenant_id = p.tenant_id
                    AND r.business_partner_id = p.id
                    AND r.role_type = ?
                    AND r.deleted_at IS NULL
              )
            """;
        int updated = jdbcTemplate.update(sql,
            command.legalName(), command.commercialName(), command.documentType(), command.documentNumber(), command.taxCondition(), command.email(),
            command.phone(), command.addressLine(), command.city(), command.provinceCode(), command.countryCode(), command.notes(), command.status(),
            actorUserId, tenantId.value(), id, command.version(), roleType
        );
        if (updated == 0) {
            return Optional.empty();
        }
        return findByIdAndRole(tenantId, id, roleType);
    }

    @Override
    public boolean removeRole(TenantId tenantId, UUID id, String roleType, UUID actorUserId, long version) {
        String partnerSql = """
            UPDATE business_partners
            SET updated_at = now(), updated_by = ?, version = version + 1
            WHERE tenant_id = ? AND id = ? AND version = ? AND deleted_at IS NULL
            """;
        int partners = jdbcTemplate.update(partnerSql, actorUserId, tenantId.value(), id, version);
        if (partners == 0) {
            return false;
        }
        String roleSql = """
            UPDATE business_partner_roles
            SET deleted_at = now(), deleted_by = ?
            WHERE tenant_id = ?
              AND business_partner_id = ?
              AND role_type = ?
              AND deleted_at IS NULL
            """;
        return jdbcTemplate.update(roleSql, actorUserId, tenantId.value(), id, roleType) > 0;
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                                 String outcome, CommercialRequestMetadata metadata, Map<String, Object> details) {
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

    private void insertRole(TenantId tenantId, UUID partnerId, UUID actorUserId, String roleType, boolean system) {
        String sql = """
            INSERT INTO business_partner_roles (tenant_id, business_partner_id, role_type, is_system, created_by)
            VALUES (?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql, tenantId.value(), partnerId, roleType, system, actorUserId);
    }

    private String partnerSelect() {
        return """
            SELECT p.id, p.tenant_id, r.role_type, p.legal_name, p.commercial_name, p.document_type, p.document_number,
                   p.tax_condition, p.email, p.phone, p.address_line, p.city, p.province_code, p.country_code, p.notes,
                   p.status, (p.is_system OR r.is_system) AS system_record, p.version, p.updated_at
            FROM business_partners p
            JOIN business_partner_roles r ON r.tenant_id = p.tenant_id AND r.business_partner_id = p.id
            """;
    }

    private CommercialPartner mapPartner(ResultSet rs, int rowNum) throws SQLException {
        return new CommercialPartner(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getString("role_type"),
            rs.getString("legal_name"),
            rs.getString("commercial_name"),
            rs.getString("document_type"),
            rs.getString("document_number"),
            rs.getString("tax_condition"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("address_line"),
            rs.getString("city"),
            rs.getString("province_code"),
            rs.getString("country_code"),
            rs.getString("notes"),
            rs.getString("status"),
            rs.getBoolean("system_record"),
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