package com.koda.platform.shared.application.tenant;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TenantContext(
    TenantId tenantId,
    UUID userId,
    Set<String> roles,
    Set<String> permissions,
    boolean platformAdmin
) {

    public TenantContext {
        Objects.requireNonNull(tenantId, "Tenant id is required");
        Objects.requireNonNull(userId, "User id is required");
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}