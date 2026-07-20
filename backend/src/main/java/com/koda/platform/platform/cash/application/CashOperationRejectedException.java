package com.koda.platform.platform.cash.application;

public class CashOperationRejectedException extends RuntimeException {

    private final String reasonCode;

    public CashOperationRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}