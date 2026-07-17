package com.koda.platform.platform.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

class JwtSecurityConfigurationTest {

    private final JwtSecurityConfiguration configuration = new JwtSecurityConfiguration();

    @Test
    void secretKeyRejectsMissingSecret() {
        KodaSecurityProperties properties = properties(null, "koda-platform");

        assertThatThrownBy(() -> configuration.jwtSecretKey(properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("KODA_JWT_SECRET is required");
    }

    @Test
    void secretKeyRejectsShortSecret() {
        KodaSecurityProperties properties = properties("too-short", "koda-platform");

        assertThatThrownBy(() -> configuration.jwtSecretKey(properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("KODA_JWT_SECRET must be at least 32 bytes for HS256");
    }

    @Test
    void decoderAcceptsExpectedIssuerAndRejectsWrongIssuer() {
        KodaSecurityProperties properties = properties("01234567890123456789012345678901", "koda-platform");
        SecretKey key = configuration.jwtSecretKey(properties);
        JwtEncoder encoder = configuration.jwtEncoder(key);
        JwtDecoder decoder = configuration.jwtDecoder(key, properties);

        String validToken = token(encoder, "koda-platform");
        String wrongIssuerToken = token(encoder, "other-issuer");

        assertThat(decoder.decode(validToken).getClaimAsString("iss")).isEqualTo("koda-platform");
        assertThatThrownBy(() -> decoder.decode(wrongIssuerToken))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void decoderRejectsMissingIssuerConfiguration() {
        KodaSecurityProperties properties = properties("01234567890123456789012345678901", " ");
        SecretKey key = configuration.jwtSecretKey(properties);

        assertThatThrownBy(() -> configuration.jwtDecoder(key, properties))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("KODA_JWT_ISSUER is required");
    }

    private KodaSecurityProperties properties(String secret, String issuer) {
        KodaSecurityProperties properties = new KodaSecurityProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setIssuer(issuer);
        return properties;
    }

    private String token(JwtEncoder encoder, String issuer) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(900))
            .subject("00000000-0000-4000-8000-000000000010")
            .claims(values -> values.putAll(Map.of("tenant_id", "00000000-0000-4000-8000-000000000001")))
            .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}