package com.koda.platform.platform.licensing.application;

public class TenantLicenseAdminNotFoundException extends RuntimeException {

    private final String resource;

    public TenantLicenseAdminNotFoundException(String resource) {
        super("License administration resource not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}
