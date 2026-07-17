package com.koda.platform.platform.catalog.application;

public record UpdateUnitOfMeasureCommand(long version, String code, String name, String symbol, Integer decimalPrecision, Boolean active) {
}
