package com.koda.platform.platform.commercial.application;

public class CommercialPartnerOperationRejectedException extends RuntimeException {

    private final String reasonCode;

    public CommercialPartnerOperationRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}