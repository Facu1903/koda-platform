package com.koda.platform.platform.cash.application;

public class CashItemNotFoundException extends RuntimeException {

    private final String resource;

    public CashItemNotFoundException(String resource) {
        super("Cash item not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}