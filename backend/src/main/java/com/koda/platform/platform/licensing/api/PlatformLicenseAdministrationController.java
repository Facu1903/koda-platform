package com.koda.platform.platform.licensing.api;

import com.koda.platform.platform.licensing.application.LicenseAdministrationRequestMetadata;
import com.koda.platform.platform.licensing.application.PlatformLicenseAdminActor;
import com.koda.platform.platform.licensing.application.PlatformLicenseAdminPermissions;
import com.koda.platform.platform.licensing.application.TenantFeatureFlagAdministration;
import com.koda.platform.platform.licensing.application.TenantLicenseAdministration;
import com.koda.platform.platform.licensing.application.TenantLicenseAdministrationService;
import com.koda.platform.platform.licensing.application.TenantLicenseTenant;
import com.koda.platform.platform.licensing.application.TenantLimitOverrideAdministration;
import com.koda.platform.platform.licensing.application.TenantModuleEntitlementAdministration;
import com.koda.platform.platform.licensing.application.TenantProductEntitlementAdministration;
import com.koda.platform.platform.licensing.application.TenantProductSubscriptionAdministration;
import com.koda.platform.platform.licensing.application.UpdateTenantModuleEntitlementCommand;
import com.koda.platform.platform.licensing.application.UpdateTenantProductEntitlementCommand;
import com.koda.platform.platform.licensing.application.UpdateTenantProductSubscriptionCommand;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.domain.tenant.TenantId;
import com.koda.platform.shared.application.security.KodaSecurityPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/platform/tenants/{tenantId}/licenses")
public class PlatformLicenseAdministrationController {

    private final TenantLicenseAdministrationService service;

    public PlatformLicenseAdministrationController(TenantLicenseAdministrationService service) {
        this.service = service;
    }

    @GetMapping
    public TenantLicenseAdministrationResponse getTenantLicenses(@PathVariable UUID tenantId, Authentication authentication) {
        return TenantLicenseAdministrationResponse.from(service.getTenantLicenses(TenantId.from(tenantId), actor(authentication)));
    }

    @PatchMapping("/subscriptions/{subscriptionId}")
    public TenantLicenseAdministrationResponse updateSubscription(
        @PathVariable UUID tenantId,
        @PathVariable UUID subscriptionId,
        @Valid @RequestBody UpdateTenantProductSubscriptionRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        TenantLicenseAdministration administration = service.updateSubscription(
            TenantId.from(tenantId),
            subscriptionId,
            request.toCommand(),
            actor(authentication),
            metadata(httpRequest)
        );
        return TenantLicenseAdministrationResponse.from(administration);
    }

    @PatchMapping("/product-entitlements/{entitlementId}")
    public TenantLicenseAdministrationResponse updateProductEntitlement(
        @PathVariable UUID tenantId,
        @PathVariable UUID entitlementId,
        @Valid @RequestBody UpdateTenantProductEntitlementRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        TenantLicenseAdministration administration = service.updateProductEntitlement(
            TenantId.from(tenantId),
            entitlementId,
            request.toCommand(),
            actor(authentication),
            metadata(httpRequest)
        );
        return TenantLicenseAdministrationResponse.from(administration);
    }

    @PatchMapping("/module-entitlements/{entitlementId}")
    public TenantLicenseAdministrationResponse updateModuleEntitlement(
        @PathVariable UUID tenantId,
        @PathVariable UUID entitlementId,
        @Valid @RequestBody UpdateTenantModuleEntitlementRequest request,
        Authentication authentication,
        HttpServletRequest httpRequest
    ) {
        TenantLicenseAdministration administration = service.updateModuleEntitlement(
            TenantId.from(tenantId),
            entitlementId,
            request.toCommand(),
            actor(authentication),
            metadata(httpRequest)
        );
        return TenantLicenseAdministrationResponse.from(administration);
    }

    private PlatformLicenseAdminActor actor(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof KodaSecurityPrincipal principal)) {
            throw new PermissionDeniedException(PlatformLicenseAdminPermissions.READ);
        }
        return new PlatformLicenseAdminActor(principal.userId(), principal.permissions(), principal.platformAdmin());
    }

    private LicenseAdministrationRequestMetadata metadata(HttpServletRequest request) {
        return new LicenseAdministrationRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record UpdateTenantProductSubscriptionRequest(
        @Min(0) long version,
        @NotBlank @Pattern(regexp = "(?i)^(ACTIVE|SUSPENDED|EXPIRED|CANCELLED)$") String status,
        Instant validUntil
    ) {
        UpdateTenantProductSubscriptionCommand toCommand() {
            return new UpdateTenantProductSubscriptionCommand(version, status, validUntil);
        }
    }

    public record UpdateTenantProductEntitlementRequest(
        @Min(0) long version,
        @NotBlank @Pattern(regexp = "(?i)^(ACTIVE|SUSPENDED|EXPIRED)$") String status,
        Instant validUntil
    ) {
        UpdateTenantProductEntitlementCommand toCommand() {
            return new UpdateTenantProductEntitlementCommand(version, status, validUntil);
        }
    }

    public record UpdateTenantModuleEntitlementRequest(
        @Min(0) long version,
        @NotBlank @Pattern(regexp = "(?i)^(ACTIVE|SUSPENDED|EXPIRED)$") String status,
        Instant validUntil
    ) {
        UpdateTenantModuleEntitlementCommand toCommand() {
            return new UpdateTenantModuleEntitlementCommand(version, status, validUntil);
        }
    }

    public record TenantLicenseAdministrationResponse(
        TenantResponse tenant,
        Instant retrievedAt,
        List<ProductSubscriptionResponse> subscriptions,
        List<ProductEntitlementResponse> productEntitlements,
        List<ModuleEntitlementResponse> moduleEntitlements,
        List<LimitOverrideResponse> limitOverrides,
        List<FeatureFlagResponse> featureFlags
    ) {
        static TenantLicenseAdministrationResponse from(TenantLicenseAdministration administration) {
            return new TenantLicenseAdministrationResponse(
                TenantResponse.from(administration.tenant()),
                administration.retrievedAt(),
                administration.subscriptions().stream().map(ProductSubscriptionResponse::from).toList(),
                administration.productEntitlements().stream().map(ProductEntitlementResponse::from).toList(),
                administration.moduleEntitlements().stream().map(ModuleEntitlementResponse::from).toList(),
                administration.limitOverrides().stream().map(LimitOverrideResponse::from).toList(),
                administration.featureFlags().stream().map(FeatureFlagResponse::from).toList()
            );
        }
    }

    public record TenantResponse(
        UUID id,
        String commercialName,
        String legalName,
        String status,
        String countryCode,
        String defaultLocale,
        String defaultCurrency,
        String timeZone,
        long version
    ) {
        static TenantResponse from(TenantLicenseTenant tenant) {
            return new TenantResponse(
                tenant.id().value(),
                tenant.commercialName(),
                tenant.legalName(),
                tenant.status(),
                tenant.countryCode(),
                tenant.defaultLocale(),
                tenant.defaultCurrency(),
                tenant.timeZone(),
                tenant.version()
            );
        }
    }

    public record ProductSubscriptionResponse(
        UUID id,
        ProductResponse product,
        PlanResponse plan,
        String status,
        Instant validFrom,
        Instant validUntil,
        String source,
        Instant cancelledAt,
        long version,
        Instant updatedAt
    ) {
        static ProductSubscriptionResponse from(TenantProductSubscriptionAdministration subscription) {
            return new ProductSubscriptionResponse(
                subscription.id(),
                new ProductResponse(subscription.productId(), subscription.productCode(), subscription.productName()),
                new PlanResponse(subscription.planId(), subscription.planCode(), subscription.planName()),
                subscription.status(),
                subscription.validFrom(),
                subscription.validUntil(),
                subscription.source(),
                subscription.cancelledAt(),
                subscription.version(),
                subscription.updatedAt()
            );
        }
    }

    public record ProductEntitlementResponse(
        UUID id,
        ProductResponse product,
        String status,
        Instant validFrom,
        Instant validUntil,
        long version,
        Instant updatedAt
    ) {
        static ProductEntitlementResponse from(TenantProductEntitlementAdministration entitlement) {
            return new ProductEntitlementResponse(
                entitlement.id(),
                new ProductResponse(entitlement.productId(), entitlement.productCode(), entitlement.productName()),
                entitlement.status(),
                entitlement.validFrom(),
                entitlement.validUntil(),
                entitlement.version(),
                entitlement.updatedAt()
            );
        }
    }

    public record ModuleEntitlementResponse(
        UUID id,
        ProductResponse product,
        ModuleResponse module,
        String status,
        Instant validFrom,
        Instant validUntil,
        long version,
        Instant updatedAt
    ) {
        static ModuleEntitlementResponse from(TenantModuleEntitlementAdministration entitlement) {
            return new ModuleEntitlementResponse(
                entitlement.id(),
                new ProductResponse(entitlement.productId(), entitlement.productCode(), entitlement.productName()),
                new ModuleResponse(entitlement.moduleId(), entitlement.moduleCode(), entitlement.moduleName(), entitlement.coreModule(),
                    entitlement.commerciallyToggleable()),
                entitlement.status(),
                entitlement.validFrom(),
                entitlement.validUntil(),
                entitlement.version(),
                entitlement.updatedAt()
            );
        }
    }

    public record LimitOverrideResponse(
        UUID id,
        ProductResponse product,
        String code,
        Long value,
        boolean unlimited,
        String unit,
        String reason,
        Instant validFrom,
        Instant validUntil,
        long version,
        Instant updatedAt
    ) {
        static LimitOverrideResponse from(TenantLimitOverrideAdministration limit) {
            return new LimitOverrideResponse(
                limit.id(),
                new ProductResponse(limit.productId(), limit.productCode(), limit.productName()),
                limit.code(),
                limit.value(),
                limit.unlimited(),
                limit.unit(),
                limit.reason(),
                limit.validFrom(),
                limit.validUntil(),
                limit.version(),
                limit.updatedAt()
            );
        }
    }

    public record FeatureFlagResponse(
        UUID id,
        ProductResponse product,
        UUID moduleId,
        String moduleCode,
        String code,
        boolean enabled,
        String reason,
        Instant validFrom,
        Instant validUntil,
        long version,
        Instant updatedAt
    ) {
        static FeatureFlagResponse from(TenantFeatureFlagAdministration featureFlag) {
            return new FeatureFlagResponse(
                featureFlag.id(),
                new ProductResponse(featureFlag.productId(), featureFlag.productCode(), featureFlag.productName()),
                featureFlag.moduleId(),
                featureFlag.moduleCode(),
                featureFlag.code(),
                featureFlag.enabled(),
                featureFlag.reason(),
                featureFlag.validFrom(),
                featureFlag.validUntil(),
                featureFlag.version(),
                featureFlag.updatedAt()
            );
        }
    }

    public record ProductResponse(UUID id, String code, String name) {
    }

    public record PlanResponse(UUID id, String code, String name) {
    }

    public record ModuleResponse(UUID id, String code, String name, boolean coreModule, boolean commerciallyToggleable) {
    }
}
