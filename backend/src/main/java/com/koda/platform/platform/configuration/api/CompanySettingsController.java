package com.koda.platform.platform.configuration.api;

import com.koda.platform.platform.configuration.application.ClientRequestMetadata;
import com.koda.platform.platform.configuration.application.CompanySettings;
import com.koda.platform.platform.configuration.application.CompanySettingsService;
import com.koda.platform.platform.configuration.application.UpdateCompanySettingsCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/company/settings")
public class CompanySettingsController {

    private final CompanySettingsService companySettingsService;

    public CompanySettingsController(CompanySettingsService companySettingsService) {
        this.companySettingsService = companySettingsService;
    }

    @GetMapping
    public CompanySettingsResponse getCurrentTenantSettings() {
        return CompanySettingsResponse.from(companySettingsService.getCurrentTenantSettings());
    }

    @PutMapping
    public CompanySettingsResponse updateCurrentTenantSettings(@Valid @RequestBody UpdateCompanySettingsRequest request,
                                                               HttpServletRequest httpRequest) {
        CompanySettings settings = companySettingsService.updateCurrentTenantSettings(request.toCommand(), metadata(httpRequest));
        return CompanySettingsResponse.from(settings);
    }

    private ClientRequestMetadata metadata(HttpServletRequest request) {
        return new ClientRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record UpdateCompanySettingsRequest(
        @NotNull @Min(0) Long version,
        @Size(max = 2048) String logoUrl,
        @Size(max = 2048) String faviconUrl,
        @Size(max = 2048) String loginImageUrl,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String primaryColor,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String secondaryColor,
        @NotBlank @Pattern(regexp = "^(light|dark|system)$") String themeMode,
        @NotBlank @Size(max = 16) String defaultLocale,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String defaultCurrency,
        @NotBlank @Size(max = 64) String timeZone,
        @NotBlank @Size(max = 32) String dateFormat,
        @NotBlank @Size(max = 32) String timeFormat,
        @NotBlank @Size(max = 16) String numberLocale,
        @NotBlank @Size(max = 32) String currencyFormat
    ) {
        UpdateCompanySettingsCommand toCommand() {
            return new UpdateCompanySettingsCommand(
                version,
                logoUrl,
                faviconUrl,
                loginImageUrl,
                primaryColor,
                secondaryColor,
                themeMode,
                defaultLocale,
                defaultCurrency,
                timeZone,
                dateFormat,
                timeFormat,
                numberLocale,
                currencyFormat
            );
        }
    }

    public record CompanySettingsResponse(
        String id,
        TenantResponse tenant,
        BrandingResponse branding,
        RegionalResponse regional,
        long version,
        Instant updatedAt
    ) {
        static CompanySettingsResponse from(CompanySettings settings) {
            return new CompanySettingsResponse(
                settings.id().toString(),
                new TenantResponse(
                    settings.tenantId().toString(),
                    settings.commercialName(),
                    settings.legalName(),
                    settings.countryCode()
                ),
                new BrandingResponse(
                    settings.logoUrl(),
                    settings.faviconUrl(),
                    settings.loginImageUrl(),
                    settings.primaryColor(),
                    settings.secondaryColor(),
                    settings.themeMode()
                ),
                new RegionalResponse(
                    settings.defaultLocale(),
                    settings.defaultCurrency(),
                    settings.timeZone(),
                    settings.dateFormat(),
                    settings.timeFormat(),
                    settings.numberLocale(),
                    settings.currencyFormat()
                ),
                settings.version(),
                settings.updatedAt()
            );
        }
    }

    public record TenantResponse(String id, String commercialName, String legalName, String countryCode) {
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