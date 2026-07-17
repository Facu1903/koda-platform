package com.koda.platform.platform.audit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.audit.application.AuditEvent;
import com.koda.platform.platform.audit.application.AuditEventFilter;
import com.koda.platform.platform.audit.application.AuditRepository;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.audit.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuditRepository implements AuditRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAuditRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AuditEvent> listEvents(TenantId tenantId, AuditEventFilter filter) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT id, tenant_id, actor_user_id, actor_type, action, resource_type, resource_id, outcome,
                   source_ip::text AS source_ip, user_agent, trace_id, metadata::text AS metadata, occurred_at
            FROM audit_events
            WHERE tenant_id = ?
            """);
        args.add(tenantId.value());
        appendUuidFilter(sql, args, "actor_user_id", filter.actorUserId());
        appendTextFilter(sql, args, "resource_type", filter.resourceType());
        appendUuidFilter(sql, args, "resource_id", filter.resourceId());
        appendTextFilter(sql, args, "action", filter.action());
        appendTextFilter(sql, args, "outcome", filter.outcome());
        if (filter.from() != null) {
            sql.append(" AND occurred_at >= ?");
            args.add(Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            sql.append(" AND occurred_at <= ?");
            args.add(Timestamp.from(filter.to()));
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ?");
        args.add(filter.limit());
        return jdbcTemplate.query(sql.toString(), this::mapAuditEvent, args.toArray());
    }

    private void appendUuidFilter(StringBuilder sql, List<Object> args, String column, UUID value) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = ?");
            args.add(value);
        }
    }

    private void appendTextFilter(StringBuilder sql, List<Object> args, String column, String value) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = ?");
            args.add(value);
        }
    }

    private AuditEvent mapAuditEvent(ResultSet rs, int rowNum) throws SQLException {
        return new AuditEvent(
            rs.getObject("id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getObject("actor_user_id", UUID.class),
            rs.getString("actor_type"),
            rs.getString("action"),
            rs.getString("resource_type"),
            rs.getObject("resource_id", UUID.class),
            rs.getString("outcome"),
            rs.getString("source_ip"),
            rs.getString("user_agent"),
            rs.getString("trace_id"),
            readMetadata(rs.getString("metadata")),
            rs.getTimestamp("occurred_at").toInstant()
        );
    }

    private Map<String, Object> readMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, METADATA_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize audit metadata", exception);
        }
    }
}