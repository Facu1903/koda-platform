package com.koda.platform.platform.licensing.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProductCapability(
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
    List<ModuleCapability> modules
) {

    public ProductCapability {
        Objects.requireNonNull(id, "Product id is required");
        requireText(code, "Product code is required");
        requireText(name, "Product name is required");
        requireText(entitlementStatus, "Product entitlement status is required");
        Objects.requireNonNull(subscriptionId, "Subscription id is required");
        requireText(subscriptionStatus, "Subscription status is required");
        requireText(planCode, "Plan code is required");
        requireText(planName, "Plan name is required");
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    public ProductCapability withModules(List<ModuleCapability> modules) {
        return new ProductCapability(id, code, name, enabled, entitlementStatus, entitlementValidFrom, entitlementValidUntil, subscriptionId,
            subscriptionStatus, subscriptionValidFrom, subscriptionValidUntil, planCode, planName, modules);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}