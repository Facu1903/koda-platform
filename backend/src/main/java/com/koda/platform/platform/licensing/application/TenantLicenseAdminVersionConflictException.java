package com.koda.platform.platform.licensing.application;

public class TenantLicenseAdminVersionConflictException extends RuntimeException {

    private final String resource;

    public TenantLicenseAdminVersionConflictException(String resource) {
        super("License administration version conflict");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}
