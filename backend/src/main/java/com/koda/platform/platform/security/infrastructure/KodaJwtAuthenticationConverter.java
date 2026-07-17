package com.koda.platform.platform.security.infrastructure;

import com.koda.platform.shared.domain.tenant.TenantId;
import com.koda.platform.shared.infrastructure.security.KodaAuthenticatedPrincipal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class KodaJwtAuthenticationConverter implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        Set<String> roles = claimSet(jwt, "roles");
        Set<String> permissions = claimSet(jwt, "permissions");
        Collection<GrantedAuthority> authorities = authorities(roles, permissions);
        String tenantId = jwt.getClaimAsString("tenant_id");

        Object principal = jwt.getSubject();
        if (tenantId != null && !tenantId.isBlank()) {
            principal = new KodaAuthenticatedPrincipal(
                UUID.fromString(jwt.getSubject()),
                TenantId.fromString(tenantId),
                jwt.getClaimAsString("email"),
                roles,
                permissions,
                Boolean.TRUE.equals(jwt.getClaim("platform_admin"))
            );
        }

        return new KodaJwtAuthenticationToken(jwt, principal, authorities);
    }

    private Set<String> claimSet(Jwt jwt, String name) {
        List<String> values = jwt.getClaimAsStringList(name);
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private Collection<GrantedAuthority> authorities(Set<String> roles, Set<String> permissions) {
        return Stream.concat(
                roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                permissions.stream().map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission))
            )
            .collect(Collectors.toUnmodifiableSet());
    }

    private static final class KodaJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

        private final Object principal;

        private KodaJwtAuthenticationToken(Jwt jwt, Object principal, Collection<GrantedAuthority> authorities) {
            super(jwt, authorities);
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public Object getCredentials() {
            return getToken().getTokenValue();
        }

        @Override
        public Map<String, Object> getTokenAttributes() {
            return getToken().getClaims();
        }
    }
}
