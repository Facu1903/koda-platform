package com.koda.platform.platform.security.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record StoredRefreshToken(
    UUID id,
    UUID userId,
    TenantId tenantId,
    Instant expiresAt
) {
}
