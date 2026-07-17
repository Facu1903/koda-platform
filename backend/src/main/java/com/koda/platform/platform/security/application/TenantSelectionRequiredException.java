package com.koda.platform.platform.security.application;

public class TenantSelectionRequiredException extends RuntimeException {

    public TenantSelectionRequiredException() {
        super("Tenant selection is required for this user");
    }
}
