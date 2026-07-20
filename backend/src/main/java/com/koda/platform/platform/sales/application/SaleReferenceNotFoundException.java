package com.koda.platform.platform.sales.application;

import java.util.UUID;

public class SaleReferenceNotFoundException extends RuntimeException {

    private final String reference;
    private final UUID referenceId;

    public SaleReferenceNotFoundException(String reference, UUID referenceId) {
        super("Sale reference not found");
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