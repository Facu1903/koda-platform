package com.koda.platform.platform.security.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final AuthTokenPolicy tokenPolicy;
    private final Clock clock;

    @Autowired
    public AuthService(
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder,
        AccessTokenIssuer accessTokenIssuer,
        RefreshTokenService refreshTokenService,
        AuthTokenPolicy tokenPolicy
    ) {
        this(authRepository, passwordEncoder, accessTokenIssuer, refreshTokenService, tokenPolicy, Clock.systemUTC());
    }

    AuthService(
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder,
        AccessTokenIssuer accessTokenIssuer,
        RefreshTokenService refreshTokenService,
        AuthTokenPolicy tokenPolicy,
        Clock clock
    ) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.tokenPolicy = tokenPolicy;
        this.clock = clock;
    }

    @Transactional
    public AuthSession login(String email, String password, Optional<TenantId> requestedTenantId, RequestMetadata metadata) {
        String normalizedEmail = normalizeEmail(email);
        Optional<UserAccount> maybeUser = authRepository.findUserByEmail(normalizedEmail);
        if (maybeUser.isEmpty() || !isActive(maybeUser.get()) || maybeUser.get().passwordHash() == null
            || !passwordEncoder.matches(password, maybeUser.get().passwordHash())) {
            authRepository.recordAuditEvent(null, null, "auth.login", "FAILURE", metadata, "{}");
            throw new AuthenticationFailedException();
        }

        UserAccount user = maybeUser.get();
        TenantAccess tenantAccess = resolveTenantAccess(user, requestedTenantId);
        AuthSession session = issueSession(user, tenantAccess, metadata);
        authRepository.recordAuditEvent(tenantAccess.tenantId(), user.id(), "auth.login", "SUCCESS", metadata, "{}");
        return session;
    }

    @Transactional
    public AuthSession refresh(String refreshToken, RequestMetadata metadata) {
        Instant now = clock.instant();
        String tokenHash = refreshTokenService.hash(refreshToken);
        StoredRefreshToken storedToken = authRepository.findActiveRefreshToken(tokenHash, now)
            .orElseThrow(InvalidRefreshTokenException::new);
        UserAccount user = authRepository.findUserById(storedToken.userId())
            .filter(this::isActive)
            .orElseThrow(InvalidRefreshTokenException::new);
        TenantAccess tenantAccess = authRepository.findActiveTenantAccess(storedToken.userId(), storedToken.tenantId())
            .orElseThrow(InvalidRefreshTokenException::new);

        AccessToken accessToken = accessTokenIssuer.createAccessToken(user, tenantAccess, now);
        RefreshTokenPair nextRefreshToken = refreshTokenService.generate(now.plus(tokenPolicy.refreshTokenTtl()));
        authRepository.storeRefreshToken(nextRefreshToken.id(), user.id(), tenantAccess.tenantId(), nextRefreshToken.hash(), now,
            nextRefreshToken.expiresAt(), metadata);
        authRepository.revokeRefreshToken(storedToken.id(), now, nextRefreshToken.id());
        authRepository.recordAuditEvent(tenantAccess.tenantId(), user.id(), "auth.refresh", "SUCCESS", metadata, "{}");

        return toSession(user, tenantAccess, accessToken, nextRefreshToken);
    }

    @Transactional
    public void logout(String refreshToken, RequestMetadata metadata) {
        String tokenHash = refreshTokenService.hash(refreshToken);
        authRepository.revokeRefreshTokenByHash(tokenHash, clock.instant());
        authRepository.recordAuditEvent(null, null, "auth.logout", "SUCCESS", metadata, "{}");
    }

    private AuthSession issueSession(UserAccount user, TenantAccess tenantAccess, RequestMetadata metadata) {
        Instant now = clock.instant();
        AccessToken accessToken = accessTokenIssuer.createAccessToken(user, tenantAccess, now);
        RefreshTokenPair refreshToken = refreshTokenService.generate(now.plus(tokenPolicy.refreshTokenTtl()));
        authRepository.storeRefreshToken(refreshToken.id(), user.id(), tenantAccess.tenantId(), refreshToken.hash(), now,
            refreshToken.expiresAt(), metadata);
        return toSession(user, tenantAccess, accessToken, refreshToken);
    }

    private AuthSession toSession(UserAccount user, TenantAccess tenantAccess, AccessToken accessToken, RefreshTokenPair refreshToken) {
        return new AuthSession(
            accessToken.value(),
            refreshToken.value(),
            accessToken.expiresAt(),
            refreshToken.expiresAt(),
            user.id(),
            user.email(),
            user.displayName(),
            tenantAccess.tenantId(),
            tenantAccess.tenantName(),
            tenantAccess.roles(),
            tenantAccess.permissions()
        );
    }

    private TenantAccess resolveTenantAccess(UserAccount user, Optional<TenantId> requestedTenantId) {
        if (requestedTenantId.isPresent()) {
            return authRepository.findActiveTenantAccess(user.id(), requestedTenantId.get())
                .orElseThrow(AuthenticationFailedException::new);
        }

        List<TenantAccess> accesses = authRepository.findActiveTenantAccesses(user.id());
        if (accesses.size() == 1) {
            return accesses.getFirst();
        }
        if (accesses.size() > 1) {
            throw new TenantSelectionRequiredException();
        }
        throw new AuthenticationFailedException();
    }

    private boolean isActive(UserAccount user) {
        return "ACTIVE".equals(user.status());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}