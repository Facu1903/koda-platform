package com.koda.platform.platform.sales.application;

public class SaleNotFoundException extends RuntimeException {

    private final String resource;

    public SaleNotFoundException(String resource) {
        super("Sale item not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}