package com.koda.platform.platform.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.koda.platform.shared.infrastructure.security.KodaAuthenticatedPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class KodaJwtAuthenticationConverterTest {

    private final KodaJwtAuthenticationConverter converter = new KodaJwtAuthenticationConverter();

    @Test
    void buildsKodaPrincipalFromTenantAwareJwt() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(900),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", userId.toString(),
                "tenant_id", tenantId.toString(),
                "email", "owner@koda.local",
                "roles", List.of("TENANT_OWNER"),
                "permissions", List.of("products:read"),
                "platform_admin", false
            )
        );

        var authentication = converter.convert(jwt);

        assertThat(authentication.getPrincipal()).isInstanceOf(KodaAuthenticatedPrincipal.class);
        KodaAuthenticatedPrincipal principal = (KodaAuthenticatedPrincipal) authentication.getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.tenantId().value()).isEqualTo(tenantId);
        assertThat(principal.roles()).containsExactly("TENANT_OWNER");
        assertThat(principal.permissions()).containsExactly("products:read");
        assertThat(authentication.getAuthorities()).extracting("authority")
            .contains("ROLE_TENANT_OWNER", "PERMISSION_products:read");
    }
}
