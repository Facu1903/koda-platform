package com.koda.platform.platform.configuration.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record CompanySettings(
    UUID id,
    TenantId tenantId,
    String commercialName,
    String legalName,
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
    long version,
    Instant updatedAt
) {
}