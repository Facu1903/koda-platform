package com.koda.platform.platform.security.infrastructure;

import com.koda.platform.platform.security.application.AccessToken;
import com.koda.platform.platform.security.application.TenantAccess;
import com.koda.platform.platform.security.application.UserAccount;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final KodaSecurityProperties properties;

    public JwtTokenService(JwtEncoder jwtEncoder, KodaSecurityProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public AccessToken createAccessToken(UserAccount user, TenantAccess tenantAccess, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.getJwt().getAccessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.getJwt().getIssuer())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.id().toString())
            .claim("tenant_id", tenantAccess.tenantId().toString())
            .claim("tenant_name", tenantAccess.tenantName())
            .claim("email", user.email())
            .claim("display_name", user.displayName())
            .claim("roles", tenantAccess.roles())
            .claim("permissions", tenantAccess.permissions())
            .claim("platform_admin", tenantAccess.platformAdmin())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }
}
