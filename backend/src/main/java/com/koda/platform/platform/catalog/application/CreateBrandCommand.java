package com.koda.platform.platform.catalog.application;

public record CreateBrandCommand(String code, String name, String description, Boolean active) {
}
