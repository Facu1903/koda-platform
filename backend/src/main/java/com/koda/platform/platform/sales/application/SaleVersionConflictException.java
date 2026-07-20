package com.koda.platform.platform.sales.application;

public class SaleVersionConflictException extends RuntimeException {

    private final String resource;

    public SaleVersionConflictException(String resource) {
        super("Sale was modified by another transaction");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}