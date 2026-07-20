package com.koda.platform.platform.cash.application;

public class CashVersionConflictException extends RuntimeException {

    private final String resource;

    public CashVersionConflictException(String resource) {
        super("Cash item was modified by another transaction");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}