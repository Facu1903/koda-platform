package com.koda.platform.platform.catalog.application;

public record UpdateBrandCommand(long version, String code, String name, String description, Boolean active) {
}
