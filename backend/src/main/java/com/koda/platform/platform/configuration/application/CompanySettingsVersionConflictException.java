package com.koda.platform.platform.configuration.application;

public class CompanySettingsVersionConflictException extends RuntimeException {

    public CompanySettingsVersionConflictException() {
        super("Company settings were modified by another transaction");
    }
}