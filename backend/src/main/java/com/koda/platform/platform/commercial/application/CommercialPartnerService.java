package com.koda.platform.platform.commercial.application;

import com.koda.platform.platform.licensing.application.LicensedModules;
import com.koda.platform.platform.licensing.application.LicensedProducts;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessGuard;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialPartnerService {

    private static final String CUSTOMER = "CUSTOMER";
    private static final String SUPPLIER = "SUPPLIER";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final int MAX_LIMIT = 500;

    private final CommercialPartnerRepository repository;
    private final CurrentTenantProvider currentTenantProvider;
    private final TenantLicenseAccessGuard licenseAccessGuard;

    public CommercialPartnerService(CommercialPartnerRepository repository, CurrentTenantProvider currentTenantProvider,
                                    TenantLicenseAccessGuard licenseAccessGuard) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
        this.licenseAccessGuard = licenseAccessGuard;
    }

    @Transactional(readOnly = true)
    public List<CommercialPartner> listCustomers(int limit) {
        TenantContext context = requirePermission("customers:read");
        return repository.listByRole(context.tenantId(), CUSTOMER, normalizeLimit(limit));
    }

    @Transactional(readOnly = true)
    public CommercialPartner getCustomer(UUID id) {
        TenantContext context = requirePermission("customers:read");
        return repository.findByIdAndRole(context.tenantId(), id, CUSTOMER).orElseThrow(() -> notFound("customer"));
    }

    @Transactional
    public CommercialPartner createCustomer(CreateCommercialPartnerCommand command, CommercialRequestMetadata metadata) {
        TenantContext context = requirePermission("customers:create");
        return createPartnerRole(context, CUSTOMER, "customer", normalize(command), metadata);
    }

    @Transactional
    public CommercialPartner updateCustomer(UUID id, UpdateCommercialPartnerCommand command, CommercialRequestMetadata metadata) {
        TenantContext context = requirePermission("customers:update");
        return updatePartnerRole(context, id, CUSTOMER, "customer", normalize(command), metadata);
    }

    @Transactional
    public void deleteCustomer(UUID id, long version, CommercialRequestMetadata metadata) {
        TenantContext context = requirePermission("customers:delete");
        deletePartnerRole(context, id, CUSTOMER, "customer", version, metadata);
    }

    @Transactional(readOnly = true)
    public List<CommercialPartner> listSuppliers(int limit) {
        TenantContext context = requirePermission("suppliers:read");
        return repository.listByRole(context.tenantId(), SUPPLIER, normalizeLimit(limit));
    }

    @Transactional(readOnly = true)
    public CommercialPartner getSupplier(UUID id) {
        TenantContext context = requirePermission("suppliers:read");
        return repository.findByIdAndRole(context.tenantId(), id, SUPPLIER).orElseThrow(() -> notFound("supplier"));
    }

    @Transactional
    public CommercialPartner createSupplier(CreateCommercialPartnerCommand command, CommercialRequestMetadata metadata) {
        TenantContext context = requirePermission("suppliers:create");
        return createPartnerRole(context, SUPPLIER, "supplier", normalize(command), metadata);
    }

    @Transactional
    public CommercialPartner updateSupplier(UUID id, UpdateCommercialPartnerCommand command, CommercialRequestMetadata metadata) {
        TenantContext context = requirePermission("suppliers:update");
        return updatePartnerRole(context, id, SUPPLIER, "supplier", normalize(command), metadata);
    }

    @Transactional
    public void deleteSupplier(UUID id, long version, CommercialRequestMetadata metadata) {
        TenantContext context = requirePermission("suppliers:delete");
        deletePartnerRole(context, id, SUPPLIER, "supplier", version, metadata);
    }

    private CommercialPartner createPartnerRole(TenantContext context, String roleType, String resource,
                                                CreateCommercialPartnerCommand command, CommercialRequestMetadata metadata) {
        TenantId tenantId = context.tenantId();
        CommercialPartner partner;
        boolean attachedToExistingPartner = false;
        if (command.documentType() != null) {
            UUID existingPartnerId = repository.findActivePartnerIdByDocument(tenantId, command.documentType(), command.documentNumber()).orElse(null);
            if (existingPartnerId != null) {
                if (repository.existsActiveRole(tenantId, existingPartnerId, roleType)) {
                    throw new CommercialPartnerOperationRejectedException("ROLE_ALREADY_EXISTS", resource + " already exists for this document");
                }
                partner = repository.addRoleToPartner(tenantId, existingPartnerId, context.userId(), roleType, false);
                attachedToExistingPartner = true;
            } else {
                partner = repository.createPartnerWithRole(tenantId, context.userId(), roleType, false, command);
            }
        } else {
            partner = repository.createPartnerWithRole(tenantId, context.userId(), roleType, false, command);
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documentType", partner.documentType());
        details.put("documentNumber", partner.documentNumber());
        details.put("attachedToExistingPartner", attachedToExistingPartner);
        audit(context, "commercial." + resource + ".create", resource, partner.id(), metadata, details);
        return partner;
    }

    private CommercialPartner updatePartnerRole(TenantContext context, UUID id, String roleType, String resource,
                                                UpdateCommercialPartnerCommand command, CommercialRequestMetadata metadata) {
        CommercialPartner existing = repository.findByIdAndRole(context.tenantId(), id, roleType).orElseThrow(() -> notFound(resource));
        rejectSystemStatusChange(existing, command.status(), resource);
        CommercialPartner updated = repository.updatePartner(context.tenantId(), id, roleType, context.userId(), command)
            .orElseThrow(() -> conflict(resource));
        audit(context, "commercial." + resource + ".update", resource, updated.id(), metadata,
            Map.of("version", updated.version(), "status", updated.status()));
        return updated;
    }

    private void deletePartnerRole(TenantContext context, UUID id, String roleType, String resource, long version, CommercialRequestMetadata metadata) {
        CommercialPartner existing = repository.findByIdAndRole(context.tenantId(), id, roleType).orElseThrow(() -> notFound(resource));
        if (existing.system()) {
            throw new CommercialPartnerOperationRejectedException("SYSTEM_PARTNER_PROTECTED", "System " + resource + " cannot be deleted");
        }
        if (!repository.removeRole(context.tenantId(), id, roleType, context.userId(), version)) {
            throw conflict(resource);
        }
        audit(context, "commercial." + resource + ".delete", resource, id, metadata, Map.of("version", version));
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        licenseAccessGuard.requireModuleEnabled(context, LicensedProducts.KODA_ERP, LicensedModules.COMMERCIAL_PARTNERS);
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return context;
        }
        throw new PermissionDeniedException(permission);
    }

    private CreateCommercialPartnerCommand normalize(CreateCommercialPartnerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Commercial partner request is required");
        }
        NormalizedDocument document = normalizeDocument(command.documentType(), command.documentNumber());
        return new CreateCommercialPartnerCommand(
            required(command.legalName(), "Legal name"),
            trimToNull(command.commercialName()),
            document.type(),
            document.number(),
            upperTrimToNull(command.taxCondition()),
            lowerTrimToNull(command.email()),
            trimToNull(command.phone()),
            trimToNull(command.addressLine()),
            trimToNull(command.city()),
            upperTrimToNull(command.provinceCode()),
            countryCode(command.countryCode()),
            trimToNull(command.notes()),
            statusOrDefault(command.status())
        );
    }

    private UpdateCommercialPartnerCommand normalize(UpdateCommercialPartnerCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Commercial partner request is required");
        }
        requireVersion(command.version());
        NormalizedDocument document = normalizeDocument(command.documentType(), command.documentNumber());
        return new UpdateCommercialPartnerCommand(
            command.version(),
            required(command.legalName(), "Legal name"),
            trimToNull(command.commercialName()),
            document.type(),
            document.number(),
            upperTrimToNull(command.taxCondition()),
            lowerTrimToNull(command.email()),
            trimToNull(command.phone()),
            trimToNull(command.addressLine()),
            trimToNull(command.city()),
            upperTrimToNull(command.provinceCode()),
            countryCode(command.countryCode()),
            trimToNull(command.notes()),
            status(command.status())
        );
    }

    private void rejectSystemStatusChange(CommercialPartner existing, String nextStatus, String resource) {
        if (existing.system() && !ACTIVE.equals(nextStatus)) {
            throw new CommercialPartnerOperationRejectedException("SYSTEM_PARTNER_PROTECTED", "System " + resource + " cannot be deactivated");
        }
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void requireVersion(long version) {
        if (version < 0) {
            throw new IllegalArgumentException("Version is required");
        }
    }

    private String required(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private NormalizedDocument normalizeDocument(String documentType, String documentNumber) {
        String type = upperTrimToNull(documentType);
        String number = trimToNull(documentNumber);
        if ((type == null && number != null) || (type != null && number == null)) {
            throw new IllegalArgumentException("Document type and document number must be provided together");
        }
        return new NormalizedDocument(type, number);
    }

    private String statusOrDefault(String value) {
        String normalized = upperTrimToNull(value);
        return normalized == null ? ACTIVE : status(normalized);
    }

    private String status(String value) {
        String normalized = upperTrimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (!normalized.equals(ACTIVE) && !normalized.equals(INACTIVE)) {
            throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE");
        }
        return normalized;
    }

    private String countryCode(String value) {
        String normalized = upperTrimToNull(value);
        if (normalized != null && !normalized.matches("^[A-Z]{2}$")) {
            throw new IllegalArgumentException("Country code must use ISO 3166-1 alpha-2 format");
        }
        return normalized;
    }

    private String upperTrimToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String lowerTrimToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void audit(TenantContext context, String action, String resourceType, UUID resourceId, CommercialRequestMetadata metadata,
                       Map<String, Object> details) {
        Map<String, Object> auditDetails = new LinkedHashMap<>(details);
        repository.recordAuditEvent(context.tenantId(), context.userId(), action, resourceType, resourceId, "SUCCESS", metadata, auditDetails);
    }

    private CommercialPartnerNotFoundException notFound(String resource) {
        return new CommercialPartnerNotFoundException(resource);
    }

    private CommercialPartnerVersionConflictException conflict(String resource) {
        return new CommercialPartnerVersionConflictException(resource);
    }

    private record NormalizedDocument(String type, String number) {
    }
}