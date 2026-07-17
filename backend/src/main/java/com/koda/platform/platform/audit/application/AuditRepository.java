package com.koda.platform.platform.audit.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.util.List;

public interface AuditRepository {

    List<AuditEvent> listEvents(TenantId tenantId, AuditEventFilter filter);
}