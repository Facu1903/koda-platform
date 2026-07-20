package com.koda.platform.platform.purchases.application;

public class PurchaseOperationRejectedException extends RuntimeException {

    private final String reasonCode;

    public PurchaseOperationRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}