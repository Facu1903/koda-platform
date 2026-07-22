package com.koda.platform.platform.audit.application;

import java.time.Instant;
import java.util.UUID;

public record AuditEventFilter(
    UUID actorUserId,
    String resourceType,
    UUID resourceId,
    String action,
    String outcome,
    Instant from,
    Instant to,
    int limit,
    Instant beforeOccurredAt,
    UUID beforeId
) {

    public AuditEventFilter(
        UUID actorUserId,
        String resourceType,
        UUID resourceId,
        String action,
        String outcome,
        Instant from,
        Instant to,
        int limit
    ) {
        this(actorUserId, resourceType, resourceId, action, outcome, from, to, limit, null, null);
    }
}
