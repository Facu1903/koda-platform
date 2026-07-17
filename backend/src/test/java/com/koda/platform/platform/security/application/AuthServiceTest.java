package com.koda.platform.platform.security.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.koda.platform.platform.security.infrastructure.JwtTokenService;
import com.koda.platform.platform.security.infrastructure.KodaSecurityProperties;
import com.koda.platform.platform.security.infrastructure.RefreshTokenGenerator;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-17T15:00:00Z");

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtTokenService jwtTokenService = Mockito.mock(JwtTokenService.class);
    private final RefreshTokenGenerator refreshTokenGenerator = Mockito.mock(RefreshTokenGenerator.class);
    private final FakeAuthRepository authRepository = new FakeAuthRepository();
    private final KodaSecurityProperties properties = new KodaSecurityProperties();
    private final RequestMetadata metadata = new RequestMetadata("127.0.0.1", "JUnit");
    private AuthService authService;

    @BeforeEach
    void setUp() {
        properties.getJwt().setRefreshTokenTtl(Duration.ofDays(30));
        authService = new AuthService(
            authRepository,
            passwordEncoder,
            jwtTokenService,
            refreshTokenGenerator,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        authRepository.user = new UserAccount(
            userId,
            "owner@koda.local",
            "KODA Owner",
            passwordEncoder.encode("CorrectPass123"),
            "ACTIVE"
        );
        authRepository.accesses = List.of(new TenantAccess(
            UUID.randomUUID(),
            tenantId,
            "KODA",
            Set.of("TENANT_OWNER"),
            Set.of("products:read"),
            false
        ));
        when(jwtTokenService.createAccessToken(any(), any(), any())).thenReturn(new AccessToken("access-token", NOW.plusSeconds(900)));
        when(refreshTokenGenerator.generate(any())).thenReturn(new RefreshTokenPair(UUID.randomUUID(), "refresh-token", "refresh-hash", NOW.plus(Duration.ofDays(30))));
        when(refreshTokenGenerator.hash("old-refresh-token")).thenReturn("old-refresh-hash");
    }

    @Test
    void loginIssuesAccessAndRefreshTokensForSingleTenantUser() {
        AuthSession session = authService.login(" OWNER@KODA.LOCAL ", "CorrectPass123", Optional.empty(), metadata);

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isEqualTo("refresh-token");
        assertThat(session.tenantId()).isEqualTo(tenantId);
        assertThat(session.roles()).containsExactly("TENANT_OWNER");
        assertThat(authRepository.storedRefreshTokens).hasSize(1);
        assertThat(authRepository.auditActions).contains("auth.login:SUCCESS");
    }

    @Test
    void loginRejectsInvalidPasswordWithoutRevealingAccountState() {
        assertThatThrownBy(() -> authService.login("owner@koda.local", "WrongPass123", Optional.empty(), metadata))
            .isInstanceOf(AuthenticationFailedException.class)
            .hasMessage("Invalid credentials");

        assertThat(authRepository.storedRefreshTokens).isEmpty();
        assertThat(authRepository.auditActions).contains("auth.login:FAILURE");
    }

    @Test
    void loginRequiresTenantSelectionWhenUserHasMultipleActiveTenants() {
        authRepository.accesses = List.of(
            authRepository.accesses.getFirst(),
            new TenantAccess(UUID.randomUUID(), TenantId.from(UUID.randomUUID()), "Other Tenant", Set.of("TENANT_ADMIN"), Set.of(), false)
        );

        assertThatThrownBy(() -> authService.login("owner@koda.local", "CorrectPass123", Optional.empty(), metadata))
            .isInstanceOf(TenantSelectionRequiredException.class);
    }

    @Test
    void refreshRotatesRefreshToken() {
        UUID oldTokenId = UUID.randomUUID();
        authRepository.storedRefreshToken = new StoredRefreshToken(oldTokenId, userId, tenantId, NOW.plus(Duration.ofDays(1)));

        AuthSession session = authService.refresh("old-refresh-token", metadata);

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.refreshToken()).isEqualTo("refresh-token");
        assertThat(authRepository.storedRefreshTokens).hasSize(1);
        assertThat(authRepository.revokedTokenIds).containsExactly(oldTokenId);
        assertThat(authRepository.auditActions).contains("auth.refresh:SUCCESS");
    }

    private final class FakeAuthRepository implements AuthRepository {
        private UserAccount user;
        private List<TenantAccess> accesses = List.of();
        private StoredRefreshToken storedRefreshToken;
        private final List<String> storedRefreshTokens = new ArrayList<>();
        private final List<UUID> revokedTokenIds = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();

        @Override
        public Optional<UserAccount> findUserByEmail(String email) {
            return user != null && user.email().equals(email) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public Optional<UserAccount> findUserById(UUID userId) {
            return user != null && user.id().equals(userId) ? Optional.of(user) : Optional.empty();
        }

        @Override
        public List<TenantAccess> findActiveTenantAccesses(UUID userId) {
            return accesses;
        }

        @Override
        public Optional<TenantAccess> findActiveTenantAccess(UUID userId, TenantId tenantId) {
            return accesses.stream().filter(access -> access.tenantId().equals(tenantId)).findFirst();
        }

        @Override
        public void storeRefreshToken(UUID tokenId, UUID userId, TenantId tenantId, String tokenHash, Instant issuedAt, Instant expiresAt,
                                      RequestMetadata metadata) {
            storedRefreshTokens.add(tokenHash);
        }

        @Override
        public Optional<StoredRefreshToken> findActiveRefreshToken(String tokenHash, Instant now) {
            return "old-refresh-hash".equals(tokenHash) ? Optional.ofNullable(storedRefreshToken) : Optional.empty();
        }

        @Override
        public void revokeRefreshToken(UUID tokenId, Instant revokedAt, UUID replacementTokenId) {
            revokedTokenIds.add(tokenId);
        }

        @Override
        public void revokeRefreshTokenByHash(String tokenHash, Instant revokedAt) {
            auditActions.add("revoke:" + tokenHash);
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String outcome, RequestMetadata metadata, String jsonMetadata) {
            auditActions.add(action + ":" + outcome);
        }
    }
}
