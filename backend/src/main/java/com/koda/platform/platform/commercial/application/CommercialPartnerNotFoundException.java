package com.koda.platform.platform.commercial.application;

public class CommercialPartnerNotFoundException extends RuntimeException {

    private final String resource;

    public CommercialPartnerNotFoundException(String resource) {
        super("Commercial partner not found");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}