package com.koda.platform.shared.infrastructure.tenant;

import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ThreadLocalCurrentTenantProvider implements CurrentTenantProvider {

    @Override
    public Optional<TenantContext> currentContext() {
        return TenantContextHolder.get();
    }
}