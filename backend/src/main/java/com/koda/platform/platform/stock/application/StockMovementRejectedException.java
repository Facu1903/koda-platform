package com.koda.platform.platform.stock.application;

public class StockMovementRejectedException extends RuntimeException {

    private final String reasonCode;

    public StockMovementRejectedException(String reasonCode, String message) {
        super(message);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}