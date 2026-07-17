package com.koda.platform.shared.application.tenant;

public class MissingTenantContextException extends RuntimeException {

    public MissingTenantContextException() {
        super("Tenant context is required for this operation");
    }
}