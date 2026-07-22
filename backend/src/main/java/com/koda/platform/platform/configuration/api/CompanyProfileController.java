package com.koda.platform.platform.configuration.api;

import com.koda.platform.platform.configuration.application.CompanyRuntimeProfile;
import com.koda.platform.platform.configuration.application.CompanySettingsService;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/company/profile")
public class CompanyProfileController {

    private final CompanySettingsService companySettingsService;

    public CompanyProfileController(CompanySettingsService companySettingsService) {
        this.companySettingsService = companySettingsService;
    }

    @GetMapping
    public CompanyProfileResponse getCurrentTenantProfile() {
        return CompanyProfileResponse.from(companySettingsService.getCurrentTenantRuntimeProfile());
    }

    public record CompanyProfileResponse(
        TenantResponse tenant,
        BrandingResponse branding,
        RegionalResponse regional,
        Instant updatedAt
    ) {
        static CompanyProfileResponse from(CompanyRuntimeProfile profile) {
            return new CompanyProfileResponse(
                new TenantResponse(
                    profile.tenantId().toString(),
                    profile.commercialName(),
                    profile.countryCode()
                ),
                new BrandingResponse(
                    profile.logoUrl(),
                    profile.faviconUrl(),
                    profile.loginImageUrl(),
                    profile.primaryColor(),
                    profile.secondaryColor(),
                    profile.themeMode()
                ),
                new RegionalResponse(
                    profile.defaultLocale(),
                    profile.defaultCurrency(),
                    profile.timeZone(),
                    profile.dateFormat(),
                    profile.timeFormat(),
                    profile.numberLocale(),
                    profile.currencyFormat()
                ),
                profile.updatedAt()
            );
        }
    }

    public record TenantResponse(String id, String commercialName, String countryCode) {
    }

    public record BrandingResponse(
        String logoUrl,
        String faviconUrl,
        String loginImageUrl,
        String primaryColor,
        String secondaryColor,
        String themeMode
    ) {
    }

    public record RegionalResponse(
        String defaultLocale,
        String defaultCurrency,
        String timeZone,
        String dateFormat,
        String timeFormat,
        String numberLocale,
        String currencyFormat
    ) {
    }
}
