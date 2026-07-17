package com.koda.platform.platform.security.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthSession(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    UUID userId,
    String email,
    String displayName,
    TenantId tenantId,
    String tenantName,
    Set<String> roles,
    Set<String> permissions
) {
}
