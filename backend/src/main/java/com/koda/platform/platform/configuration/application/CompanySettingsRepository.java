package com.koda.platform.platform.configuration.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CompanySettingsRepository {

    Optional<CompanySettings> findByTenantId(TenantId tenantId);

    Optional<CompanySettings> update(TenantId tenantId, UUID actorUserId, UpdateCompanySettingsCommand command);

    void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String outcome, UUID resourceId,
                          ClientRequestMetadata metadata, Map<String, Object> details);
}