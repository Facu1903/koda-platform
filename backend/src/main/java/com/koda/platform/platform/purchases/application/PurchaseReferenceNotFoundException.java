package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public class PurchaseReferenceNotFoundException extends RuntimeException {

    private final String reference;
    private final UUID referenceId;

    public PurchaseReferenceNotFoundException(String reference, UUID referenceId) {
        super("Purchase reference not found");
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