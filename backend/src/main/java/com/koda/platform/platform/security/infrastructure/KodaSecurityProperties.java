package com.koda.platform.platform.security.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "koda.security")
public class KodaSecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Bootstrap bootstrap = new Bootstrap();

    public Jwt getJwt() {
        return jwt;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public static class Jwt {
        private String issuer = "koda-platform";
        private String secret;
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(30);

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }
    }

    public static class Bootstrap {
        private String ownerEmail;
        private String ownerPassword;
        private String ownerDisplayName = "KODA Owner";
        private String tenantId = "00000000-0000-4000-8000-000000000001";

        public String getOwnerEmail() {
            return ownerEmail;
        }

        public void setOwnerEmail(String ownerEmail) {
            this.ownerEmail = ownerEmail;
        }

        public String getOwnerPassword() {
            return ownerPassword;
        }

        public void setOwnerPassword(String ownerPassword) {
            this.ownerPassword = ownerPassword;
        }

        public String getOwnerDisplayName() {
            return ownerDisplayName;
        }

        public void setOwnerDisplayName(String ownerDisplayName) {
            this.ownerDisplayName = ownerDisplayName;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }
}
