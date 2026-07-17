package com.koda.platform.platform.stock.application;

public class StockItemNotFoundException extends RuntimeException {

    private final String resource;

    public StockItemNotFoundException(String resource) {
        super("Stock item not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}