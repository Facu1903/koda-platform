package com.koda.platform.platform.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.platform.licensing.application.TenantLicenseAccessDeniedException;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessGuard;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.testing.FakeTenantLicenseAccessRepository;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final FakeAuditRepository repository = new FakeAuditRepository();
    private final FakeTenantLicenseAccessRepository licenseAccessRepository = new FakeTenantLicenseAccessRepository();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(repository, currentTenantProvider, new TenantLicenseAccessGuard(licenseAccessRepository), Duration.ofDays(90));
        currentTenantProvider.context = Optional.of(new TenantContext(
            tenantId,
            userId,
            Set.of("TENANT_ADMIN"),
            Set.of("audit:read"),
            false
        ));
        repository.events.add(event("stock.movement.create", "stock_movement", "SUCCESS"));
    }

    @Test
    void listEventsRequiresAuditReadPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of(), Set.of(), false));

        assertThatThrownBy(() -> service.listEvents(defaultFilter()))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void auditModuleDisabledBlocksAuditReadEvenWithPermission() {
        licenseAccessRepository.disableModule();

        assertThatThrownBy(() -> service.listEvents(defaultFilter()))
            .isInstanceOf(TenantLicenseAccessDeniedException.class)
            .satisfies(exception -> assertThat(((TenantLicenseAccessDeniedException) exception).reasonCode()).isEqualTo("MODULE_NOT_ENABLED"));
    }

    @Test
    void listEventsUsesCurrentTenantAndReturnsRepositoryResults() {
        List<AuditEvent> events = service.listEvents(defaultFilter());

        assertThat(events).hasSize(1);
        assertThat(repository.lastTenantId).isEqualTo(tenantId);
    }

    @Test
    void listEventsNormalizesOutcomeAndTextFilters() {
        service.listEvents(new AuditEventFilter(null, " stock_movement ", null, " stock.movement.create ", " success ", null, null, 100));

        assertThat(repository.lastFilter.resourceType()).isEqualTo("stock_movement");
        assertThat(repository.lastFilter.action()).isEqualTo("stock.movement.create");
        assertThat(repository.lastFilter.outcome()).isEqualTo("SUCCESS");
    }

    @Test
    void listEventsCapsLimitAtFiveHundred() {
        service.listEvents(new AuditEventFilter(null, null, null, null, null, null, null, 900));

        assertThat(repository.lastFilter.limit()).isEqualTo(500);
    }

    @Test
    void listEventsRejectsInvalidOutcome() {
        assertThatThrownBy(() -> service.listEvents(new AuditEventFilter(null, null, null, null, "UNKNOWN", null, null, 100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Audit outcome must be SUCCESS or FAILURE");
    }

    @Test
    void listEventsRejectsInvertedDateRange() {
        Instant from = Instant.parse("2026-07-17T12:00:00Z");
        Instant to = Instant.parse("2026-07-17T11:00:00Z");

        assertThatThrownBy(() -> service.listEvents(new AuditEventFilter(null, null, null, null, null, from, to, 100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Audit from date cannot be after to date");
    }

    @Test
    void listEventsRejectsDateRangeWiderThanOperationalPolicy() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-01T00:00:00Z");

        assertThatThrownBy(() -> service.listEvents(new AuditEventFilter(null, null, null, null, null, from, to, 100)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Audit date range exceeds the operational maximum");
    }

    @Test
    void listEventsRequiresCompleteCursor() {
        Instant beforeOccurredAt = Instant.parse("2026-07-17T12:00:00Z");

        assertThatThrownBy(() -> service.listEvents(new AuditEventFilter(null, null, null, null, null, null, null, 100, beforeOccurredAt, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Audit cursor requires beforeOccurredAt and beforeId");

        assertThatThrownBy(() -> service.listEvents(new AuditEventFilter(null, null, null, null, null, null, null, 100, null, UUID.randomUUID())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Audit cursor requires beforeOccurredAt and beforeId");
    }

    @Test
    void listEventsPassesStableCursorToRepository() {
        Instant beforeOccurredAt = Instant.parse("2026-07-17T12:00:00Z");
        UUID beforeId = UUID.fromString("10000000-0000-4000-8000-000000000001");

        service.listEvents(new AuditEventFilter(null, null, null, null, null, null, null, 100, beforeOccurredAt, beforeId));

        assertThat(repository.lastFilter.beforeOccurredAt()).isEqualTo(beforeOccurredAt);
        assertThat(repository.lastFilter.beforeId()).isEqualTo(beforeId);
    }

    private AuditEventFilter defaultFilter() {
        return new AuditEventFilter(null, null, null, null, null, null, null, 100);
    }

    private AuditEvent event(String action, String resourceType, String outcome) {
        return new AuditEvent(
            UUID.randomUUID(),
            tenantId,
            userId,
            "USER",
            action,
            resourceType,
            UUID.randomUUID(),
            outcome,
            "127.0.0.1",
            "JUnit",
            null,
            Map.of("source", "test"),
            Instant.parse("2026-07-17T18:00:00Z")
        );
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakeAuditRepository implements AuditRepository {
        private final List<AuditEvent> events = new ArrayList<>();
        private TenantId lastTenantId;
        private AuditEventFilter lastFilter;

        @Override
        public List<AuditEvent> listEvents(TenantId tenantId, AuditEventFilter filter) {
            this.lastTenantId = tenantId;
            this.lastFilter = filter;
            return events.stream()
                .filter(event -> event.tenantId().equals(tenantId))
                .limit(filter.limit())
                .toList();
        }
    }
}
