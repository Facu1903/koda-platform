package com.koda.platform.platform.sales.application;

public class SaleOperationRejectedException extends RuntimeException {

    private final String reasonCode;

    public SaleOperationRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}