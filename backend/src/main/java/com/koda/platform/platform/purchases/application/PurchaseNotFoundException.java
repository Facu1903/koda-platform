package com.koda.platform.platform.purchases.application;

public class PurchaseNotFoundException extends RuntimeException {

    private final String resource;

    public PurchaseNotFoundException(String resource) {
        super("Purchase item not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}