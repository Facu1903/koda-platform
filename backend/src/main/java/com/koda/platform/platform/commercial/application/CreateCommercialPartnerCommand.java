package com.koda.platform.platform.commercial.application;

public record CreateCommercialPartnerCommand(
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
    String status
) {
}