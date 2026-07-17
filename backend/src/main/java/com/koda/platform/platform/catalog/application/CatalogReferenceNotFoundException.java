package com.koda.platform.platform.catalog.application;

import java.util.UUID;

public class CatalogReferenceNotFoundException extends RuntimeException {

    private final String reference;
    private final UUID referenceId;

    public CatalogReferenceNotFoundException(String reference, UUID referenceId) {
        super("Catalog reference not found");
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