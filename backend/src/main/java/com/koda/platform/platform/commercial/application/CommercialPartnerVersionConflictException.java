package com.koda.platform.platform.commercial.application;

public class CommercialPartnerVersionConflictException extends RuntimeException {

    private final String resource;

    public CommercialPartnerVersionConflictException(String resource) {
        super("Commercial partner was modified by another transaction");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}