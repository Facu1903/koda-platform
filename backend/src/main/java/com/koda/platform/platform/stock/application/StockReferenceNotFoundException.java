package com.koda.platform.platform.stock.application;

import java.util.UUID;

public class StockReferenceNotFoundException extends RuntimeException {

    private final String reference;
    private final UUID referenceId;

    public StockReferenceNotFoundException(String reference, UUID referenceId) {
        super("Stock reference not found");
        this.reference = reference;
        this.referenceId = referenceId;
    }

    public String reference() {
        return reference;
    }

    public UUID referenceId() {
        return referenceId;
    }
}