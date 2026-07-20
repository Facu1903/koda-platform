package com.koda.platform.platform.licensing.application;

public class TenantLicenseAccessDeniedException extends RuntimeException {

    private final String reasonCode;
    private final String productCode;
    private final String moduleCode;

    public TenantLicenseAccessDeniedException(String reasonCode, String productCode, String moduleCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
        this.productCode = productCode;
        this.moduleCode = moduleCode;
    }

    public String reasonCode() {
        return reasonCode;
    }

    public String productCode() {
        return productCode;
    }

    public String moduleCode() {
        return moduleCode;
    }
}
