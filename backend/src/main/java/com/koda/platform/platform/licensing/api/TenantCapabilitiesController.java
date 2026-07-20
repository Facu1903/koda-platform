package com.koda.platform.platform.licensing.api;

import com.koda.platform.platform.licensing.application.FeatureFlagCapability;
import com.koda.platform.platform.licensing.application.LimitCapability;
import com.koda.platform.platform.licensing.application.ModuleCapability;
import com.koda.platform.platform.licensing.application.ProductCapability;
import com.koda.platform.platform.licensing.application.TenantCapabilities;
import com.koda.platform.platform.licensing.application.TenantCapabilitiesService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/capabilities")
public class TenantCapabilitiesController {

    private final TenantCapabilitiesService tenantCapabilitiesService;

    public TenantCapabilitiesController(TenantCapabilitiesService tenantCapabilitiesService) {
        this.tenantCapabilitiesService = tenantCapabilitiesService;
    }

    @GetMapping
    public TenantCapabilitiesResponse currentTenantCapabilities() {
        return TenantCapabilitiesResponse.from(tenantCapabilitiesService.currentTenantCapabilities());
    }

    public record TenantCapabilitiesResponse(
        UUID tenantId,
        boolean tenantActive,
        Instant calculatedAt,
        List<ProductCapabilityResponse> products,
        List<FeatureFlagCapabilityResponse> featureFlags,
        List<LimitCapabilityResponse> limits
    ) {
        static TenantCapabilitiesResponse from(TenantCapabilities capabilities) {
            return new TenantCapabilitiesResponse(
                capabilities.tenantId().value(),
                capabilities.tenantActive(),
                capabilities.calculatedAt(),
                capabilities.products().stream().map(ProductCapabilityResponse::from).toList(),
                capabilities.featureFlags().stream().map(FeatureFlagCapabilityResponse::from).toList(),
                capabilities.limits().stream().map(LimitCapabilityResponse::from).toList()
            );
        }
    }

    public record ProductCapabilityResponse(
        UUID id,
        String code,
        String name,
        boolean enabled,
        String entitlementStatus,
        Instant entitlementValidFrom,
        Instant entitlementValidUntil,
        UUID subscriptionId,
        String subscriptionStatus,
        Instant subscriptionValidFrom,
        Instant subscriptionValidUntil,
        String planCode,
        String planName,
        List<ModuleCapabilityResponse> modules
    ) {
        static ProductCapabilityResponse from(ProductCapability product) {
            return new ProductCapabilityResponse(
                product.id(),
                product.code(),
                product.name(),
                product.enabled(),
                product.entitlementStatus(),
                product.entitlementValidFrom(),
                product.entitlementValidUntil(),
                product.subscriptionId(),
                product.subscriptionStatus(),
                product.subscriptionValidFrom(),
                product.subscriptionValidUntil(),
                product.planCode(),
                product.planName(),
                product.modules().stream().map(ModuleCapabilityResponse::from).toList()
            );
        }
    }

    public record ModuleCapabilityResponse(
        UUID id,
        String productCode,
        String code,
        String name,
        boolean enabled,
        boolean coreModule,
        boolean commerciallyToggleable,
        String entitlementStatus,
        Instant validFrom,
        Instant validUntil
    ) {
        static ModuleCapabilityResponse from(ModuleCapability module) {
            return new ModuleCapabilityResponse(
                module.id(),
                module.productCode(),
                module.code(),
                module.name(),
                module.enabled(),
                module.coreModule(),
                module.commerciallyToggleable(),
                module.entitlementStatus(),
                module.validFrom(),
                module.validUntil()
            );
        }
    }

    public record FeatureFlagCapabilityResponse(
        String productCode,
        String moduleCode,
        String code,
        boolean enabled,
        Instant validFrom,
        Instant validUntil
    ) {
        static FeatureFlagCapabilityResponse from(FeatureFlagCapability featureFlag) {
            return new FeatureFlagCapabilityResponse(
                featureFlag.productCode(),
                featureFlag.moduleCode(),
                featureFlag.code(),
                featureFlag.enabled(),
                featureFlag.validFrom(),
                featureFlag.validUntil()
            );
        }
    }

    public record LimitCapabilityResponse(
        String productCode,
        String code,
        Long value,
        boolean unlimited,
        String unit,
        String source
    ) {
        static LimitCapabilityResponse from(LimitCapability limit) {
            return new LimitCapabilityResponse(limit.productCode(), limit.code(), limit.value(), limit.unlimited(), limit.unit(), limit.source());
        }
    }
}