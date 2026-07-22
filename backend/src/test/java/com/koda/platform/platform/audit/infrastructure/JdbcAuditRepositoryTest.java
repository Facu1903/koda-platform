package com.koda.platform.platform.audit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koda.platform.platform.audit.application.AuditEventFilter;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcAuditRepositoryTest {

    @Test
    void listEventsUsesStableKeysetCursorWhenProvided() {
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
        JdbcAuditRepository repository = new JdbcAuditRepository(jdbcTemplate, new ObjectMapper());
        TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
        Instant beforeOccurredAt = Instant.parse("2026-07-22T12:00:00Z");
        UUID beforeId = UUID.fromString("10000000-0000-4000-8000-000000000001");

        repository.listEvents(tenantId, new AuditEventFilter(null, null, null, null, null, null, null, 50, beforeOccurredAt, beforeId));

        assertThat(jdbcTemplate.sql)
            .contains("WHERE tenant_id = ?")
            .contains("AND (occurred_at < ? OR (occurred_at = ? AND id < ?))")
            .contains("ORDER BY occurred_at DESC, id DESC LIMIT ?");
        assertThat(jdbcTemplate.args).containsExactly(
            tenantId.value(),
            java.sql.Timestamp.from(beforeOccurredAt),
            java.sql.Timestamp.from(beforeOccurredAt),
            beforeId,
            50
        );
    }

    private static final class CapturingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private Object[] args;

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            this.args = args;
            return List.of();
        }
    }
}
