package com.koda.platform.platform.licensing.application;

public class TenantCapabilitiesUnavailableException extends RuntimeException {

    private final String reasonCode;

    public TenantCapabilitiesUnavailableException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}