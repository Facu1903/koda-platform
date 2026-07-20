package com.koda.platform.platform.commercial.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.platform.licensing.application.TenantLicenseAccessDeniedException;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessGuard;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.testing.FakeTenantLicenseAccessRepository;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommercialPartnerServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final TenantId otherTenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000099"));
    private final UUID userId = UUID.randomUUID();
    private final FakeCommercialPartnerRepository repository = new FakeCommercialPartnerRepository();
    private final FakeTenantLicenseAccessRepository licenseAccessRepository = new FakeTenantLicenseAccessRepository();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final CommercialRequestMetadata metadata = new CommercialRequestMetadata("127.0.0.1", "JUnit");
    private CommercialPartnerService service;

    @BeforeEach
    void setUp() {
        service = new CommercialPartnerService(repository, currentTenantProvider, new TenantLicenseAccessGuard(licenseAccessRepository));
        currentTenantProvider.context = Optional.of(new TenantContext(
            tenantId,
            userId,
            Set.of("TENANT_ADMIN"),
            Set.of(
                "customers:read", "customers:create", "customers:update", "customers:delete",
                "suppliers:read", "suppliers:create", "suppliers:update", "suppliers:delete"
            ),
            false
        ));
    }

    @Test
    void createCustomerNormalizesFieldsAndRecordsAudit() {
        CommercialPartner customer = service.createCustomer(new CreateCommercialPartnerCommand(
            "  Acme SA  ",
            " Acme ",
            " cuit ",
            " 30-11111111-1 ",
            " responsable_inscripto ",
            " INFO@ACME.COM ",
            " 123 ",
            " Calle 1 ",
            " Buenos Aires ",
            " ba ",
            " ar ",
            "  Nota ",
            null
        ), metadata);

        assertThat(customer.legalName()).isEqualTo("Acme SA");
        assertThat(customer.documentType()).isEqualTo("CUIT");
        assertThat(customer.taxCondition()).isEqualTo("RESPONSABLE_INSCRIPTO");
        assertThat(customer.email()).isEqualTo("info@acme.com");
        assertThat(customer.provinceCode()).isEqualTo("BA");
        assertThat(customer.countryCode()).isEqualTo("AR");
        assertThat(customer.status()).isEqualTo("ACTIVE");
        assertThat(repository.auditActions).contains("commercial.customer.create:customer");
    }

    @Test
    void createCustomerRejectsMissingPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of(), Set.of("customers:read"), false));

        assertThatThrownBy(() -> service.createCustomer(minimalPartner("Cliente"), metadata))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void commercialPartnersModuleDisabledBlocksOperationsEvenWithPermission() {
        licenseAccessRepository.disableModule();

        assertThatThrownBy(() -> service.listCustomers(100))
            .isInstanceOf(TenantLicenseAccessDeniedException.class)
            .satisfies(exception -> assertThat(((TenantLicenseAccessDeniedException) exception).reasonCode()).isEqualTo("MODULE_NOT_ENABLED"));
    }

    @Test
    void listSuppliersUsesCurrentTenantOnly() {
        repository.partners.add(new PartnerState(UUID.randomUUID(), otherTenantId, Set.of("SUPPLIER"), "Proveedor externo", null, null, null,
            null, null, null, null, null, null, null, null, "ACTIVE", false, 0));
        CommercialPartner ownSupplier = service.createSupplier(minimalPartner("Proveedor propio"), metadata);

        List<CommercialPartner> suppliers = service.listSuppliers(100);

        assertThat(suppliers).extracting(CommercialPartner::tenantId).containsOnly(tenantId);
        assertThat(suppliers).extracting(CommercialPartner::id).contains(ownSupplier.id());
    }

    @Test
    void updateSupplierRejectsStaleVersion() {
        CommercialPartner supplier = service.createSupplier(minimalPartner("Proveedor"), metadata);

        assertThatThrownBy(() -> service.updateSupplier(supplier.id(), new UpdateCommercialPartnerCommand(
            99,
            "Proveedor",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "ACTIVE"
        ), metadata)).isInstanceOf(CommercialPartnerVersionConflictException.class);
    }

    @Test
    void systemCustomerCannotBeDeactivatedOrDeleted() {
        UUID consumerId = repository.addSystemCustomer(tenantId);

        assertThatThrownBy(() -> service.updateCustomer(consumerId, new UpdateCommercialPartnerCommand(
            0,
            "Consumidor Final",
            "Consumidor Final",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "INACTIVE"
        ), metadata)).isInstanceOf(CommercialPartnerOperationRejectedException.class)
            .hasMessage("System customer cannot be deactivated");

        assertThatThrownBy(() -> service.deleteCustomer(consumerId, 0, metadata))
            .isInstanceOf(CommercialPartnerOperationRejectedException.class)
            .hasMessage("System customer cannot be deleted");
    }

    @Test
    void supplierWithExistingCustomerDocumentAttachesRoleToSamePartner() {
        CommercialPartner customer = service.createCustomer(new CreateCommercialPartnerCommand(
            "ACME", null, "CUIT", "30-22222222-2", null, null, null, null, null, null, "AR", null, "ACTIVE"
        ), metadata);

        CommercialPartner supplier = service.createSupplier(new CreateCommercialPartnerCommand(
            "ACME Supplier", null, "CUIT", "30-22222222-2", null, null, null, null, null, null, "AR", null, "ACTIVE"
        ), metadata);

        assertThat(supplier.id()).isEqualTo(customer.id());
        assertThat(supplier.roleType()).isEqualTo("SUPPLIER");
        assertThat(repository.auditActions).contains("commercial.supplier.create:supplier");
    }

    @Test
    void deleteCustomerRemovesOnlyCustomerRole() {
        CommercialPartner customer = service.createCustomer(minimalPartner("Cliente"), metadata);

        service.deleteCustomer(customer.id(), customer.version(), metadata);

        assertThat(repository.findByIdAndRole(tenantId, customer.id(), "CUSTOMER")).isEmpty();
        assertThat(repository.auditActions).contains("commercial.customer.delete:customer");
    }

    private CreateCommercialPartnerCommand minimalPartner(String legalName) {
        return new CreateCommercialPartnerCommand(legalName, null, null, null, null, null, null, null, null, null, "AR", null, null);
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakeCommercialPartnerRepository implements CommercialPartnerRepository {
        private final List<PartnerState> partners = new ArrayList<>();
        private final List<String> auditActions = new ArrayList<>();

        @Override
        public List<CommercialPartner> listByRole(TenantId tenantId, String roleType, int limit) {
            return partners.stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.roles().contains(roleType))
                .limit(limit)
                .map(item -> item.toPartner(roleType))
                .toList();
        }

        @Override
        public Optional<CommercialPartner> findByIdAndRole(TenantId tenantId, UUID id, String roleType) {
            return partners.stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id) && item.roles().contains(roleType))
                .map(item -> item.toPartner(roleType))
                .findFirst();
        }

        @Override
        public Optional<UUID> findActivePartnerIdByDocument(TenantId tenantId, String documentType, String documentNumber) {
            return partners.stream()
                .filter(item -> item.tenantId().equals(tenantId))
                .filter(item -> documentType.equals(item.documentType()) && documentNumber.equals(item.documentNumber()))
                .map(PartnerState::id)
                .findFirst();
        }

        @Override
        public boolean existsActiveRole(TenantId tenantId, UUID partnerId, String roleType) {
            return partners.stream().anyMatch(item -> item.tenantId().equals(tenantId) && item.id().equals(partnerId) && item.roles().contains(roleType));
        }

        @Override
        public CommercialPartner createPartnerWithRole(TenantId tenantId, UUID actorUserId, String roleType, boolean system,
                                                       CreateCommercialPartnerCommand command) {
            PartnerState state = new PartnerState(UUID.randomUUID(), tenantId, Set.of(roleType), command.legalName(), command.commercialName(),
                command.documentType(), command.documentNumber(), command.taxCondition(), command.email(), command.phone(), command.addressLine(),
                command.city(), command.provinceCode(), command.countryCode(), command.notes(), command.status(), system, 0);
            partners.add(state);
            return state.toPartner(roleType);
        }

        @Override
        public CommercialPartner addRoleToPartner(TenantId tenantId, UUID partnerId, UUID actorUserId, String roleType, boolean system) {
            PartnerState existing = state(tenantId, partnerId);
            PartnerState updated = existing.withRole(roleType);
            partners.remove(existing);
            partners.add(updated);
            return updated.toPartner(roleType);
        }

        @Override
        public Optional<CommercialPartner> updatePartner(TenantId tenantId, UUID id, String roleType, UUID actorUserId,
                                                         UpdateCommercialPartnerCommand command) {
            Optional<PartnerState> existing = partners.stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id) && item.roles().contains(roleType))
                .findFirst();
            if (existing.isEmpty() || existing.get().version() != command.version()) {
                return Optional.empty();
            }
            partners.remove(existing.get());
            PartnerState updated = existing.get().update(command);
            partners.add(updated);
            return Optional.of(updated.toPartner(roleType));
        }

        @Override
        public boolean removeRole(TenantId tenantId, UUID id, String roleType, UUID actorUserId, long version) {
            Optional<PartnerState> existing = partners.stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id) && item.roles().contains(roleType))
                .findFirst();
            if (existing.isEmpty() || existing.get().version() != version) {
                return false;
            }
            partners.remove(existing.get());
            partners.add(existing.get().withoutRole(roleType));
            return true;
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId, String outcome,
                                     CommercialRequestMetadata metadata, Map<String, Object> details) {
            auditActions.add(action + ":" + resourceType);
        }

        private UUID addSystemCustomer(TenantId tenantId) {
            UUID id = UUID.randomUUID();
            partners.add(new PartnerState(id, tenantId, Set.of("CUSTOMER"), "Consumidor Final", "Consumidor Final", null, null, null, null, null,
                null, null, null, null, null, "ACTIVE", true, 0));
            return id;
        }

        private PartnerState state(TenantId tenantId, UUID id) {
            return partners.stream()
                .filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id))
                .findFirst()
                .orElseThrow();
        }
    }

    private record PartnerState(
        UUID id,
        TenantId tenantId,
        Set<String> roles,
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
        long version
    ) {
        CommercialPartner toPartner(String roleType) {
            return new CommercialPartner(id, tenantId, roleType, legalName, commercialName, documentType, documentNumber, taxCondition, email, phone,
                addressLine, city, provinceCode, countryCode, notes, status, system, version, Instant.parse("2026-07-20T10:00:00Z"));
        }

        PartnerState withRole(String roleType) {
            java.util.Set<String> updatedRoles = new java.util.LinkedHashSet<>(roles);
            updatedRoles.add(roleType);
            return new PartnerState(id, tenantId, Set.copyOf(updatedRoles), legalName, commercialName, documentType, documentNumber, taxCondition, email,
                phone, addressLine, city, provinceCode, countryCode, notes, status, system, version + 1);
        }

        PartnerState withoutRole(String roleType) {
            java.util.Set<String> updatedRoles = new java.util.LinkedHashSet<>(roles);
            updatedRoles.remove(roleType);
            return new PartnerState(id, tenantId, Set.copyOf(updatedRoles), legalName, commercialName, documentType, documentNumber, taxCondition, email,
                phone, addressLine, city, provinceCode, countryCode, notes, status, system, version + 1);
        }

        PartnerState update(UpdateCommercialPartnerCommand command) {
            return new PartnerState(id, tenantId, roles, command.legalName(), command.commercialName(), command.documentType(), command.documentNumber(),
                command.taxCondition(), command.email(), command.phone(), command.addressLine(), command.city(), command.provinceCode(), command.countryCode(),
                command.notes(), command.status(), system, version + 1);
        }
    }
}