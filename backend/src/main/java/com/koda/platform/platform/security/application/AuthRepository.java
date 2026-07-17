package com.koda.platform.platform.security.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthRepository {

    Optional<UserAccount> findUserByEmail(String email);

    Optional<UserAccount> findUserById(UUID userId);

    List<TenantAccess> findActiveTenantAccesses(UUID userId);

    Optional<TenantAccess> findActiveTenantAccess(UUID userId, TenantId tenantId);

    void storeRefreshToken(UUID tokenId, UUID userId, TenantId tenantId, String tokenHash, Instant issuedAt, Instant expiresAt,
                           RequestMetadata metadata);

    Optional<StoredRefreshToken> findActiveRefreshToken(String tokenHash, Instant now);

    void revokeRefreshToken(UUID tokenId, Instant revokedAt, UUID replacementTokenId);

    void revokeRefreshTokenByHash(String tokenHash, Instant revokedAt);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String outcome, RequestMetadata metadata, String jsonMetadata);
}
