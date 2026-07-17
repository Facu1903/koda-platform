package com.koda.platform.platform.catalog.application;

public record UpdateCategoryCommand(long version, String code, String name, String description, Boolean active) {
}
