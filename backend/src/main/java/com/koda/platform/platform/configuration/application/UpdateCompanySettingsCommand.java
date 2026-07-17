package com.koda.platform.platform.configuration.application;

public record UpdateCompanySettingsCommand(
    Long version,
    String logoUrl,
    String faviconUrl,
    String loginImageUrl,
    String primaryColor,
    String secondaryColor,
    String themeMode,
    String defaultLocale,
    String defaultCurrency,
    String timeZone,
    String dateFormat,
    String timeFormat,
    String numberLocale,
    String currencyFormat
) {
}