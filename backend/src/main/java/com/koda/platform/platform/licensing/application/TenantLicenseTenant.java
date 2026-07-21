package com.koda.platform.platform.licensing.application;

import com.koda.platform.shared.domain.tenant.TenantId;

public record TenantLicenseTenant(
    TenantId id,
    String commercialName,
    String legalName,
    String status,
    String countryCode,
    String defaultLocale,
    String defaultCurrency,
    String timeZone,
    long version
) {
}
