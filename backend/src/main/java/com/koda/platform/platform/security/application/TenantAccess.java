package com.koda.platform.platform.security.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Set;
import java.util.UUID;

public record TenantAccess(
    UUID membershipId,
    TenantId tenantId,
    String tenantName,
    Set<String> roles,
    Set<String> permissions,
    boolean platformAdmin
) {
    public TenantAccess {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
