package com.koda.platform.platform.audit.application;

import com.koda.platform.platform.licensing.application.LicensedModules;
import com.koda.platform.platform.licensing.application.LicensedProducts;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessGuard;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final String READ_PERMISSION = "audit:read";
    private static final int MAX_LIMIT = 500;
    private static final Duration DEFAULT_MAX_QUERY_RANGE = Duration.ofDays(90);

    private final AuditRepository repository;
    private final CurrentTenantProvider currentTenantProvider;
    private final TenantLicenseAccessGuard licenseAccessGuard;
    private final Duration maxQueryRange;

    @Autowired
    public AuditService(
        AuditRepository repository,
        CurrentTenantProvider currentTenantProvider,
        TenantLicenseAccessGuard licenseAccessGuard,
        @Value("${koda.audit.query.max-range:P90D}") Duration maxQueryRange
    ) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
        this.licenseAccessGuard = licenseAccessGuard;
        this.maxQueryRange = normalizeMaxQueryRange(maxQueryRange);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> listEvents(AuditEventFilter filter) {
        TenantContext context = requirePermission(READ_PERMISSION);
        return repository.listEvents(context.tenantId(), normalizeAndValidate(filter));
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        licenseAccessGuard.requireModuleEnabled(context, LicensedProducts.KODA_ERP, LicensedModules.AUDIT);
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return context;
        }
        throw new PermissionDeniedException(permission);
    }

    private AuditEventFilter normalizeAndValidate(AuditEventFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Audit filter is required");
        }
        Instant from = filter.from();
        Instant to = filter.to();
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Audit from date cannot be after to date");
        }
        if (from != null && to != null && Duration.between(from, to).compareTo(maxQueryRange) > 0) {
            throw new IllegalArgumentException("Audit date range exceeds the operational maximum");
        }
        if ((filter.beforeOccurredAt() == null) != (filter.beforeId() == null)) {
            throw new IllegalArgumentException("Audit cursor requires beforeOccurredAt and beforeId");
        }
        String outcome = trimToNull(filter.outcome());
        if (outcome != null) {
            outcome = outcome.toUpperCase(Locale.ROOT);
            if (!outcome.equals("SUCCESS") && !outcome.equals("FAILURE")) {
                throw new IllegalArgumentException("Audit outcome must be SUCCESS or FAILURE");
            }
        }
        int limit = normalizeLimit(filter.limit());
        return new AuditEventFilter(
            filter.actorUserId(),
            trimToNull(filter.resourceType()),
            filter.resourceId(),
            trimToNull(filter.action()),
            outcome,
            from,
            to,
            limit,
            filter.beforeOccurredAt(),
            filter.beforeId()
        );
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Duration normalizeMaxQueryRange(Duration maxQueryRange) {
        if (maxQueryRange == null || maxQueryRange.isZero() || maxQueryRange.isNegative()) {
            return DEFAULT_MAX_QUERY_RANGE;
        }
        return maxQueryRange;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
