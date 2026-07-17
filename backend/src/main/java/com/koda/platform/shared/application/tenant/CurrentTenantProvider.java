package com.koda.platform.shared.application.tenant;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Optional;
import java.util.UUID;

public interface CurrentTenantProvider {

    Optional<TenantContext> currentContext();

    default TenantContext requireContext() {
        return currentContext().orElseThrow(MissingTenantContextException::new);
    }

    default Optional<TenantId> currentTenantId() {
        return currentContext().map(TenantContext::tenantId);
    }

    default TenantId requireTenantId() {
        return requireContext().tenantId();
    }

    default UUID requireUserId() {
        return requireContext().userId();
    }
}