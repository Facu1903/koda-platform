package com.koda.platform.platform.purchases.application;

public class PurchaseVersionConflictException extends RuntimeException {

    private final String resource;

    public PurchaseVersionConflictException(String resource) {
        super("Purchase was modified by another transaction");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}