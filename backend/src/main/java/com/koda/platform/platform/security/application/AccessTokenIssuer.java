package com.koda.platform.platform.security.application;

import java.time.Instant;

public interface AccessTokenIssuer {

    AccessToken createAccessToken(UserAccount user, TenantAccess tenantAccess, Instant issuedAt);
}