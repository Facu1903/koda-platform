package com.koda.platform.shared.application.security;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Set;
import java.util.UUID;

public interface KodaSecurityPrincipal {

    UUID userId();

    TenantId tenantId();

    Set<String> roles();

    Set<String> permissions();

    boolean platformAdmin();
}
