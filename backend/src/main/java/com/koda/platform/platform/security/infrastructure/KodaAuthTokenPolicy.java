package com.koda.platform.platform.security.infrastructure;

import com.koda.platform.platform.security.application.AuthTokenPolicy;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class KodaAuthTokenPolicy implements AuthTokenPolicy {

    private final KodaSecurityProperties properties;

    public KodaAuthTokenPolicy(KodaSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public Duration refreshTokenTtl() {
        return properties.getJwt().getRefreshTokenTtl();
    }
}