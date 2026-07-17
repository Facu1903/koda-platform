package com.koda.platform.platform.catalog.application;

public record CreateUnitOfMeasureCommand(String code, String name, String symbol, Integer decimalPrecision, Boolean active) {
}
