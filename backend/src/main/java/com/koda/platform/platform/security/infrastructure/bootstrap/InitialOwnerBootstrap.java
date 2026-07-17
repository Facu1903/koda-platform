package com.koda.platform.platform.security.infrastructure.bootstrap;

import com.koda.platform.platform.security.infrastructure.KodaSecurityProperties;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "koda.security.auth.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitialOwnerBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialOwnerBootstrap.class);
    private static final String TENANT_OWNER_ROLE = "TENANT_OWNER";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final KodaSecurityProperties properties;

    public InitialOwnerBootstrap(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, KodaSecurityProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        KodaSecurityProperties.Bootstrap bootstrap = properties.getBootstrap();
        boolean hasEmail = hasText(bootstrap.getOwnerEmail());
        boolean hasPassword = hasText(bootstrap.getOwnerPassword());
        if (!hasEmail && !hasPassword) {
            return;
        }
        if (!hasEmail || !hasPassword) {
            throw new IllegalStateException("Both KODA_BOOTSTRAP_OWNER_EMAIL and KODA_BOOTSTRAP_OWNER_PASSWORD are required for bootstrap");
        }
        if (bootstrap.getOwnerPassword().length() < 12) {
            throw new IllegalStateException("KODA_BOOTSTRAP_OWNER_PASSWORD must contain at least 12 characters");
        }

        TenantId tenantId = TenantId.fromString(bootstrap.getTenantId());
        String email = bootstrap.getOwnerEmail().trim().toLowerCase(Locale.ROOT);
        String displayName = hasText(bootstrap.getOwnerDisplayName()) ? bootstrap.getOwnerDisplayName().trim() : "KODA Owner";

        ensureTenantExists(tenantId);
        UUID userId = ensureUser(email, displayName, bootstrap.getOwnerPassword());
        UUID membershipId = ensureMembership(userId, tenantId);
        ensureTenantOwnerRole(membershipId, tenantId);
        LOGGER.info("Initial owner bootstrap ensured for configured tenant");
    }

    private void ensureTenantExists(TenantId tenantId) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM tenants WHERE id = ? AND deleted_at IS NULL", Integer.class, tenantId.value());
        if (count == null || count == 0) {
            throw new IllegalStateException("Bootstrap tenant does not exist");
        }
    }

    private UUID ensureUser(String email, String displayName, String password) {
        Optional<UUID> existingUserId = jdbcTemplate.query(
            "SELECT id FROM user_accounts WHERE email = ? AND deleted_at IS NULL LIMIT 1",
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            email
        ).stream().findFirst();

        if (existingUserId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE user_accounts
                SET password_hash = COALESCE(password_hash, ?), status = 'ACTIVE', updated_at = now()
                WHERE id = ?
                """, passwordEncoder.encode(password), existingUserId.get());
            return existingUserId.get();
        }

        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO user_accounts (id, email, display_name, password_hash, status, locale, time_zone)
            VALUES (?, ?, ?, ?, 'ACTIVE', 'es-AR', 'America/Argentina/Buenos_Aires')
            """, userId, email, displayName, passwordEncoder.encode(password));
        return userId;
    }

    private UUID ensureMembership(UUID userId, TenantId tenantId) {
        Optional<UUID> existingMembershipId = jdbcTemplate.query("""
            SELECT id
            FROM tenant_memberships
            WHERE tenant_id = ? AND user_id = ? AND deleted_at IS NULL
            LIMIT 1
            """, (rs, rowNum) -> rs.getObject("id", UUID.class), tenantId.value(), userId).stream().findFirst();

        if (existingMembershipId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE tenant_memberships
                SET status = 'ACTIVE', joined_at = COALESCE(joined_at, now()), updated_at = now()
                WHERE id = ?
                """, existingMembershipId.get());
            return existingMembershipId.get();
        }

        UUID membershipId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO tenant_memberships (id, tenant_id, user_id, status, joined_at)
            VALUES (?, ?, ?, 'ACTIVE', now())
            """, membershipId, tenantId.value(), userId);
        return membershipId;
    }

    private void ensureTenantOwnerRole(UUID membershipId, TenantId tenantId) {
        UUID roleId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM roles
            WHERE tenant_id = ? AND code = ? AND scope = 'TENANT' AND deleted_at IS NULL
            """, UUID.class, tenantId.value(), TENANT_OWNER_ROLE);

        Integer count = jdbcTemplate.queryForObject("""
            SELECT count(*)
            FROM tenant_membership_roles
            WHERE tenant_id = ? AND membership_id = ? AND role_id = ?
            """, Integer.class, tenantId.value(), membershipId, roleId);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update("""
            INSERT INTO tenant_membership_roles (tenant_id, membership_id, role_id)
            VALUES (?, ?, ?)
            """, tenantId.value(), membershipId, roleId);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
