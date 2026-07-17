package com.koda.platform.platform.security.api;

import com.koda.platform.platform.security.application.AuthService;
import com.koda.platform.platform.security.application.AuthSession;
import com.koda.platform.platform.security.application.RequestMetadata;
import com.koda.platform.shared.domain.tenant.TenantId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthSession session = authService.login(
            request.email(),
            request.password(),
            Optional.ofNullable(request.tenantId()).filter(value -> !value.isBlank()).map(TenantId::fromString),
            metadata(httpRequest)
        );
        return AuthResponse.from(session);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return AuthResponse.from(authService.refresh(request.refreshToken(), metadata(httpRequest)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        authService.logout(request.refreshToken(), metadata(httpRequest));
    }

    private RequestMetadata metadata(HttpServletRequest request) {
        return new RequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        String tenantId
    ) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserResponse user,
        TenantResponse tenant
    ) {
        static AuthResponse from(AuthSession session) {
            long expiresInSeconds = Math.max(0, Duration.between(java.time.Instant.now(), session.accessTokenExpiresAt()).toSeconds());
            return new AuthResponse(
                "Bearer",
                session.accessToken(),
                session.refreshToken(),
                expiresInSeconds,
                new UserResponse(session.userId().toString(), session.email(), session.displayName(), session.roles(), session.permissions()),
                new TenantResponse(session.tenantId().toString(), session.tenantName())
            );
        }
    }

    public record UserResponse(String id, String email, String displayName, java.util.Set<String> roles, java.util.Set<String> permissions) {
    }

    public record TenantResponse(String id, String name) {
    }
}
