package com.koda.platform.platform.audit.api;

import com.koda.platform.platform.audit.application.AuditEvent;
import com.koda.platform.platform.audit.application.AuditEventFilter;
import com.koda.platform.platform.audit.application.AuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    public List<AuditEventResponse> listEvents(
        @RequestParam(required = false) UUID actorUserId,
        @RequestParam(required = false) @Size(max = 120) String resourceType,
        @RequestParam(required = false) UUID resourceId,
        @RequestParam(required = false) @Size(max = 120) String action,
        @RequestParam(required = false) @Pattern(regexp = "(?i)SUCCESS|FAILURE") String outcome,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        AuditEventFilter filter = new AuditEventFilter(actorUserId, resourceType, resourceId, action, outcome, from, to, limit);
        return auditService.listEvents(filter).stream().map(AuditEventResponse::from).toList();
    }

    public record AuditEventResponse(
        UUID id,
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
        static AuditEventResponse from(AuditEvent event) {
            return new AuditEventResponse(
                event.id(),
                event.actorUserId(),
                event.actorType(),
                event.action(),
                event.resourceType(),
                event.resourceId(),
                event.outcome(),
                event.sourceIp(),
                event.userAgent(),
                event.traceId(),
                event.metadata(),
                event.occurredAt()
            );
        }
    }
}