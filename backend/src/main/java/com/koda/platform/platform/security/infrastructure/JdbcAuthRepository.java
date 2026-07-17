package com.koda.platform.platform.security.infrastructure;

import com.koda.platform.platform.security.application.AuthRepository;
import com.koda.platform.platform.security.application.RequestMetadata;
import com.koda.platform.platform.security.application.StoredRefreshToken;
import com.koda.platform.platform.security.application.TenantAccess;
import com.koda.platform.platform.security.application.UserAccount;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "koda.security.auth.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAuthRepository implements AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findUserByEmail(String email) {
        String sql = """
            SELECT id, email, display_name, password_hash, status
            FROM user_accounts
            WHERE email = ? AND deleted_at IS NULL
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, this::mapUserAccount, email).stream().findFirst();
    }

    @Override
    public Optional<UserAccount> findUserById(UUID userId) {
        String sql = """
            SELECT id, email, display_name, password_hash, status
            FROM user_accounts
            WHERE id = ? AND deleted_at IS NULL
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, this::mapUserAccount, userId).stream().findFirst();
    }

    @Override
    public List<TenantAccess> findActiveTenantAccesses(UUID userId) {
        return jdbcTemplate.query(tenantAccessSql("tm.user_id = ?"), this::mapTenantAccess, userId);
    }

    @Override
    public Optional<TenantAccess> findActiveTenantAccess(UUID userId, TenantId tenantId) {
        return jdbcTemplate.query(tenantAccessSql("tm.user_id = ? AND tm.tenant_id = ?"), this::mapTenantAccess, userId, tenantId.value())
            .stream()
            .findFirst();
    }

    @Override
    public void storeRefreshToken(UUID tokenId, UUID userId, TenantId tenantId, String tokenHash, Instant issuedAt, Instant expiresAt,
                                  RequestMetadata metadata) {
        String sql = """
            INSERT INTO refresh_tokens (id, user_id, tenant_id, token_hash, issued_at, expires_at, source_ip, user_agent)
            VALUES (?, ?, ?, ?, ?, ?, CAST(? AS inet), ?)
            """;
        jdbcTemplate.update(sql, tokenId, userId, tenantId.value(), tokenHash, Timestamp.from(issuedAt), Timestamp.from(expiresAt),
            metadata.sourceIp(), metadata.userAgent());
    }

    @Override
    public Optional<StoredRefreshToken> findActiveRefreshToken(String tokenHash, Instant now) {
        String sql = """
            SELECT id, user_id, tenant_id, expires_at
            FROM refresh_tokens
            WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > ?
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, this::mapStoredRefreshToken, tokenHash, Timestamp.from(now)).stream().findFirst();
    }

    @Override
    public void revokeRefreshToken(UUID tokenId, Instant revokedAt, UUID replacementTokenId) {
        String sql = """
            UPDATE refresh_tokens
            SET revoked_at = ?, replaced_by_token_id = ?
            WHERE id = ? AND revoked_at IS NULL
            """;
        jdbcTemplate.update(sql, Timestamp.from(revokedAt), replacementTokenId, tokenId);
    }

    @Override
    public void revokeRefreshTokenByHash(String tokenHash, Instant revokedAt) {
        String sql = """
            UPDATE refresh_tokens
            SET revoked_at = ?
            WHERE token_hash = ? AND revoked_at IS NULL
            """;
        jdbcTemplate.update(sql, Timestamp.from(revokedAt), tokenHash);
    }

    @Override
    public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String outcome, RequestMetadata metadata, String jsonMetadata) {
        String sql = """
            INSERT INTO audit_events (
                tenant_id, actor_user_id, actor_type, action, resource_type, outcome, source_ip, user_agent, metadata
            ) VALUES (?, ?, ?, ?, 'auth', ?, CAST(? AS inet), ?, CAST(? AS jsonb))
            """;
        jdbcTemplate.update(sql,
            tenantId == null ? null : tenantId.value(),
            actorUserId,
            actorUserId == null ? "SYSTEM" : "USER",
            action,
            outcome,
            metadata.sourceIp(),
            metadata.userAgent(),
            jsonMetadata == null ? "{}" : jsonMetadata
        );
    }

    private String tenantAccessSql(String predicate) {
        return """
            SELECT
                tm.id AS membership_id,
                t.id AS tenant_id,
                t.commercial_name AS tenant_name,
                COALESCE(string_agg(DISTINCT r.code, ','), '') AS roles_csv,
                COALESCE(string_agg(DISTINCT p.code, ','), '') AS permissions_csv
            FROM tenant_memberships tm
            JOIN tenants t ON t.id = tm.tenant_id
            LEFT JOIN tenant_membership_roles tmr ON tmr.membership_id = tm.id AND tmr.tenant_id = tm.tenant_id
            LEFT JOIN roles r ON r.id = tmr.role_id AND r.tenant_id = tm.tenant_id AND r.deleted_at IS NULL
            LEFT JOIN role_permissions rp ON rp.role_id = r.id
            LEFT JOIN permissions p ON p.id = rp.permission_id
            WHERE tm.status = 'ACTIVE'
              AND tm.deleted_at IS NULL
              AND t.status = 'ACTIVE'
              AND t.deleted_at IS NULL
              AND %s
            GROUP BY tm.id, t.id, t.commercial_name
            ORDER BY t.commercial_name
            """.formatted(predicate);
    }

    private UserAccount mapUserAccount(ResultSet rs, int rowNum) throws SQLException {
        return new UserAccount(
            rs.getObject("id", UUID.class),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getString("password_hash"),
            rs.getString("status")
        );
    }

    private TenantAccess mapTenantAccess(ResultSet rs, int rowNum) throws SQLException {
        Set<String> roles = csvToSet(rs.getString("roles_csv"));
        return new TenantAccess(
            rs.getObject("membership_id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getString("tenant_name"),
            roles,
            csvToSet(rs.getString("permissions_csv")),
            roles.contains("PLATFORM_SUPER_ADMIN")
        );
    }

    private StoredRefreshToken mapStoredRefreshToken(ResultSet rs, int rowNum) throws SQLException {
        return new StoredRefreshToken(
            rs.getObject("id", UUID.class),
            rs.getObject("user_id", UUID.class),
            TenantId.from(rs.getObject("tenant_id", UUID.class)),
            rs.getTimestamp("expires_at").toInstant()
        );
    }

    private Set<String> csvToSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    }
}
