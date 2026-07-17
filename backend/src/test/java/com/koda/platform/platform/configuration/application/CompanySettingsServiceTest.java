package com.koda.platform.platform.configuration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanySettingsServiceTest {

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.randomUUID();
    private final FakeCompanySettingsRepository repository = new FakeCompanySettingsRepository();
    private final FakeCurrentTenantProvider currentTenantProvider = new FakeCurrentTenantProvider();
    private final ClientRequestMetadata metadata = new ClientRequestMetadata("127.0.0.1", "JUnit");
    private CompanySettingsService service;

    @BeforeEach
    void setUp() {
        service = new CompanySettingsService(repository, currentTenantProvider);
        repository.settings = settings(0, "#F6862B", null, "dark", "es-AR", "ARS", "America/Argentina/Buenos_Aires");
        currentTenantProvider.context = Optional.of(new TenantContext(
            tenantId,
            userId,
            Set.of("TENANT_ADMIN"),
            Set.of("company_settings:read", "company_settings:update"),
            false
        ));
    }

    @Test
    void getCurrentTenantSettingsReturnsSettingsForTenantWithPermission() {
        CompanySettings settings = service.getCurrentTenantSettings();

        assertThat(settings.tenantId()).isEqualTo(tenantId);
        assertThat(settings.primaryColor()).isEqualTo("#F6862B");
        assertThat(repository.requestedTenantId).isEqualTo(tenantId);
    }

    @Test
    void getCurrentTenantSettingsRejectsMissingReadPermission() {
        currentTenantProvider.context = Optional.of(new TenantContext(tenantId, userId, Set.of(), Set.of(), false));

        assertThatThrownBy(() -> service.getCurrentTenantSettings())
            .isInstanceOf(PermissionDeniedException.class)
            .hasMessage("Permission denied");
    }

    @Test
    void updateCurrentTenantSettingsNormalizesValuesAndRecordsAudit() {
        UpdateCompanySettingsCommand command = new UpdateCompanySettingsCommand(
            0L,
            "  https://cdn.koda.local/logo.png  ",
            " ",
            "https://cdn.koda.local/login.png",
            "#f6862b",
            "#ffffff",
            "DARK",
            "es_AR",
            "ars",
            "America/Argentina/Buenos_Aires",
            "dd/MM/yyyy",
            "HH:mm",
            "es_AR",
            "symbol"
        );

        CompanySettings updated = service.updateCurrentTenantSettings(command, metadata);

        assertThat(updated.logoUrl()).isEqualTo("https://cdn.koda.local/logo.png");
        assertThat(updated.faviconUrl()).isNull();
        assertThat(updated.primaryColor()).isEqualTo("#F6862B");
        assertThat(updated.secondaryColor()).isEqualTo("#FFFFFF");
        assertThat(updated.themeMode()).isEqualTo("dark");
        assertThat(updated.defaultCurrency()).isEqualTo("ARS");
        assertThat(updated.defaultLocale()).isEqualTo("es-AR");
        assertThat(updated.numberLocale()).isEqualTo("es-AR");
        assertThat(updated.version()).isEqualTo(1);
        assertThat(repository.auditAction).isEqualTo("company_settings.update");
        assertThat(repository.auditResourceId).isEqualTo(updated.id());
        assertThat(repository.auditDetails).containsEntry("beforeVersion", 0L).containsEntry("afterVersion", 1L);
        Map<?, ?> changedFields = (Map<?, ?>) repository.auditDetails.get("changedFields");
        assertThat(changedFields.containsKey("logoUrl")).isTrue();
        assertThat(changedFields.containsKey("secondaryColor")).isTrue();
    }

    @Test
    void updateCurrentTenantSettingsRejectsStaleVersion() {
        UpdateCompanySettingsCommand command = validCommand(99L);

        assertThatThrownBy(() -> service.updateCurrentTenantSettings(command, metadata))
            .isInstanceOf(CompanySettingsVersionConflictException.class);
    }

    @Test
    void updateCurrentTenantSettingsRejectsInvalidRegionalConfiguration() {
        UpdateCompanySettingsCommand command = new UpdateCompanySettingsCommand(
            0L,
            null,
            null,
            null,
            "#F6862B",
            null,
            "dark",
            "es-AR",
            "ZZZ",
            "No/SuchZone",
            "dd/MM/yyyy",
            "HH:mm",
            "es-AR",
            "symbol"
        );

        assertThatThrownBy(() -> service.updateCurrentTenantSettings(command, metadata))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private UpdateCompanySettingsCommand validCommand(long version) {
        return new UpdateCompanySettingsCommand(
            version,
            null,
            null,
            null,
            "#F6862B",
            null,
            "dark",
            "es-AR",
            "ARS",
            "America/Argentina/Buenos_Aires",
            "dd/MM/yyyy",
            "HH:mm",
            "es-AR",
            "symbol"
        );
    }

    private CompanySettings settings(long version, String primaryColor, String secondaryColor, String themeMode, String defaultLocale,
                                     String defaultCurrency, String timeZone) {
        return new CompanySettings(
            UUID.fromString("00000000-0000-4000-8000-000000000002"),
            tenantId,
            "KODA",
            "KODA",
            "AR",
            defaultLocale,
            defaultCurrency,
            timeZone,
            null,
            null,
            null,
            primaryColor,
            secondaryColor,
            themeMode,
            "dd/MM/yyyy",
            "HH:mm",
            defaultLocale,
            "symbol",
            version,
            Instant.parse("2026-07-17T15:00:00Z")
        );
    }

    private final class FakeCurrentTenantProvider implements CurrentTenantProvider {
        private Optional<TenantContext> context = Optional.empty();

        @Override
        public Optional<TenantContext> currentContext() {
            return context;
        }
    }

    private final class FakeCompanySettingsRepository implements CompanySettingsRepository {
        private CompanySettings settings;
        private TenantId requestedTenantId;
        private String auditAction;
        private UUID auditResourceId;
        private Map<String, Object> auditDetails;

        @Override
        public Optional<CompanySettings> findByTenantId(TenantId tenantId) {
            requestedTenantId = tenantId;
            return Optional.ofNullable(settings).filter(value -> value.tenantId().equals(tenantId));
        }

        @Override
        public Optional<CompanySettings> update(TenantId tenantId, UUID actorUserId, UpdateCompanySettingsCommand command) {
            if (!settings.tenantId().equals(tenantId) || settings.version() != command.version()) {
                return Optional.empty();
            }
            settings = new CompanySettings(
                settings.id(),
                settings.tenantId(),
                settings.commercialName(),
                settings.legalName(),
                settings.countryCode(),
                command.defaultLocale(),
                command.defaultCurrency(),
                command.timeZone(),
                command.logoUrl(),
                command.faviconUrl(),
                command.loginImageUrl(),
                command.primaryColor(),
                command.secondaryColor(),
                command.themeMode(),
                command.dateFormat(),
                command.timeFormat(),
                command.numberLocale(),
                command.currencyFormat(),
                settings.version() + 1,
                Instant.parse("2026-07-17T16:00:00Z")
            );
            return Optional.of(settings);
        }

        @Override
        public void recordAuditEvent(TenantId tenantId, UUID actorUserId, String action, String outcome, UUID resourceId,
                                     ClientRequestMetadata metadata, Map<String, Object> details) {
            auditAction = action;
            auditResourceId = resourceId;
            auditDetails = details;
        }
    }
}