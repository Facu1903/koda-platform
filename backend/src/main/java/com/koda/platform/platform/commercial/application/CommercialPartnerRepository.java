package com.koda.platform.platform.commercial.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CommercialPartnerRepository {

    List<CommercialPartner> listByRole(TenantId tenantId, String roleType, int limit);

    Optional<CommercialPartner> findByIdAndRole(TenantId tenantId, UUID id, String roleType);

    Optional<UUID> findActivePartnerIdByDocument(TenantId tenantId, String documentType, String documentNumber);

    boolean existsActiveRole(TenantId tenantId, UUID partnerId, String roleType);

    CommercialPartner createPartnerWithRole(TenantId tenantId, UUID actorUserId, String roleType, boolean system,
                                            CreateCommercialPartnerCommand command);

    CommercialPartner addRoleToPartner(TenantId tenantId, UUID partnerId, UUID actorUserId, String roleType, boolean system);

    Optional<CommercialPartner> updatePartner(TenantId tenantId, UUID id, String roleType, UUID actorUserId,
                                              UpdateCommercialPartnerCommand command);

    boolean removeRole(TenantId tenantId, UUID id, String roleType, UUID actorUserId, long version);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String resourceType, UUID resourceId,
                          String outcome, CommercialRequestMetadata metadata, Map<String, Object> details);
}