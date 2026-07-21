package com.koda.platform.shared.infrastructure.security;

import com.koda.platform.shared.application.security.KodaSecurityPrincipal;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record KodaAuthenticatedPrincipal(
    UUID userId,
    TenantId tenantId,
    String email,
    Set<String> roles,
    Set<String> permissions,
    boolean platformAdmin
) implements KodaSecurityPrincipal {

    public KodaAuthenticatedPrincipal {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(tenantId, "Tenant id is required");
        Objects.requireNonNull(email, "Email is required");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}