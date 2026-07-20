package com.koda.platform.platform.commercial.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.UUID;

public record CommercialPartner(
    UUID id,
    TenantId tenantId,
    String roleType,
    String legalName,
    String commercialName,
    String documentType,
    String documentNumber,
    String taxCondition,
    String email,
    String phone,
    String addressLine,
    String city,
    String provinceCode,
    String countryCode,
    String notes,
    String status,
    boolean system,
    long version,
    Instant updatedAt
) {
}