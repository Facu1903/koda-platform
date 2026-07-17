package com.koda.platform.platform.catalog.application;

public class CatalogItemNotFoundException extends RuntimeException {

    private final String resource;

    public CatalogItemNotFoundException(String resource) {
        super("Catalog item not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}