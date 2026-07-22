package com.koda.platform.platform.configuration.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;

public record CompanyRuntimeProfile(
    TenantId tenantId,
    String commercialName,
    String countryCode,
    String defaultLocale,
    String defaultCurrency,
    String timeZone,
    String logoUrl,
    String faviconUrl,
    String loginImageUrl,
    String primaryColor,
    String secondaryColor,
    String themeMode,
    String dateFormat,
    String timeFormat,
    String numberLocale,
    String currencyFormat,
    Instant updatedAt
) {

    static CompanyRuntimeProfile from(CompanySettings settings) {
        return new CompanyRuntimeProfile(
            settings.tenantId(),
            settings.commercialName(),
            settings.countryCode(),
            settings.defaultLocale(),
            settings.defaultCurrency(),
            settings.timeZone(),
            settings.logoUrl(),
            settings.faviconUrl(),
            settings.loginImageUrl(),
            settings.primaryColor(),
            settings.secondaryColor(),
            settings.themeMode(),
            settings.dateFormat(),
            settings.timeFormat(),
            settings.numberLocale(),
            settings.currencyFormat(),
            settings.updatedAt()
        );
    }
}
