package com.koda.platform.platform.catalog.application;

public record CreateCategoryCommand(String code, String name, String description, Boolean active) {
}
