package com.koda.platform.platform.licensing.application;

public class TenantLicenseAdminOperationRejectedException extends RuntimeException {

    private final String reasonCode;

    public TenantLicenseAdminOperationRejectedException(String reasonCode) {
        super("License administration operation rejected");
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
