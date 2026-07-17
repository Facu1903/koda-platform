package com.koda.platform.platform.catalog.application;

public class CatalogVersionConflictException extends RuntimeException {

    private final String resource;

    public CatalogVersionConflictException(String resource) {
        super("Catalog item was modified by another transaction");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}