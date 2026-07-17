package com.koda.platform.platform.audit.application;

import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
    UUID id,
    TenantId tenantId,
    UUID actorUserId,
    String actorType,
    String action,
    String resourceType,
    UUID resourceId,
    String outcome,
    String sourceIp,
    String userAgent,
    String traceId,
    Map<String, Object> metadata,
    Instant occurredAt
) {
}