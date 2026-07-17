package com.koda.platform.platform.configuration.application;

public class CompanySettingsNotFoundException extends RuntimeException {

    public CompanySettingsNotFoundException() {
        super("Company settings not found");
    }
}