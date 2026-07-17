package com.koda.platform.platform.configuration.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanySettingsService {

    static final String READ_PERMISSION = "company_settings:read";
    static final String UPDATE_PERMISSION = "company_settings:update";

    private final CompanySettingsRepository repository;
    private final CurrentTenantProvider currentTenantProvider;

    public CompanySettingsService(CompanySettingsRepository repository, CurrentTenantProvider currentTenantProvider) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Transactional(readOnly = true)
    public CompanySettings getCurrentTenantSettings() {
        TenantContext context = currentTenantProvider.requireContext();
        requirePermission(context, READ_PERMISSION);
        return repository.findByTenantId(context.tenantId())
            .orElseThrow(CompanySettingsNotFoundException::new);
    }

    @Transactional
    public CompanySettings updateCurrentTenantSettings(UpdateCompanySettingsCommand command, ClientRequestMetadata metadata) {
        TenantContext context = currentTenantProvider.requireContext();
        requirePermission(context, UPDATE_PERMISSION);
        UpdateCompanySettingsCommand normalized = normalizeAndValidate(command);
        TenantId tenantId = context.tenantId();
        CompanySettings before = repository.findByTenantId(tenantId)
            .orElseThrow(CompanySettingsNotFoundException::new);
        CompanySettings updated = repository.update(tenantId, context.userId(), normalized)
            .orElseThrow(CompanySettingsVersionConflictException::new);
        repository.recordAuditEvent(tenantId, context.userId(), "company_settings.update", "SUCCESS", updated.id(), metadata,
            auditDetails(before, updated));
        return updated;
    }

    private void requirePermission(TenantContext context, String permission) {
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return;
        }
        throw new PermissionDeniedException(permission);
    }

    private UpdateCompanySettingsCommand normalizeAndValidate(UpdateCompanySettingsCommand command) {
        if (command.version() == null || command.version() < 0) {
            throw new IllegalArgumentException("Company settings version is required");
        }

        String primaryColor = normalizeColor(command.primaryColor(), true);
        String secondaryColor = normalizeColor(command.secondaryColor(), false);
        String themeMode = required(command.themeMode(), "Theme mode").toLowerCase(Locale.ROOT);
        if (!themeMode.equals("light") && !themeMode.equals("dark") && !themeMode.equals("system")) {
            throw new IllegalArgumentException("Theme mode must be light, dark or system");
        }

        String defaultLocale = normalizeLocale(command.defaultLocale(), "Default locale");
        String numberLocale = normalizeLocale(command.numberLocale(), "Number locale");
        String defaultCurrency = required(command.defaultCurrency(), "Default currency").toUpperCase(Locale.ROOT);
        Currency.getInstance(defaultCurrency);
        String timeZone = required(command.timeZone(), "Time zone");
        ZoneId.of(timeZone);
        String dateFormat = required(command.dateFormat(), "Date format");
        DateTimeFormatter.ofPattern(dateFormat);
        String timeFormat = required(command.timeFormat(), "Time format");
        DateTimeFormatter.ofPattern(timeFormat);
        String currencyFormat = required(command.currencyFormat(), "Currency format");

        return new UpdateCompanySettingsCommand(
            command.version(),
            trimToNull(command.logoUrl()),
            trimToNull(command.faviconUrl()),
            trimToNull(command.loginImageUrl()),
            primaryColor,
            secondaryColor,
            themeMode,
            defaultLocale,
            defaultCurrency,
            timeZone,
            dateFormat,
            timeFormat,
            numberLocale,
            currencyFormat
        );
    }

    private String normalizeColor(String value, boolean required) {
        String normalized = required ? required(value, "Color") : trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Color must use #RRGGBB format");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeLocale(String value, String fieldName) {
        String normalized = required(value, fieldName).replace('_', '-');
        if (!normalized.matches("^[a-z]{2,3}(-[A-Z]{2})?$")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }

    private String required(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> auditDetails(CompanySettings before, CompanySettings updated) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("beforeVersion", before.version());
        details.put("afterVersion", updated.version());
        details.put("changedFields", changedFields(before, updated));
        return details;
    }

    private Map<String, Object> changedFields(CompanySettings before, CompanySettings updated) {
        Map<String, Object> fields = new LinkedHashMap<>();
        addIfChanged(fields, "defaultLocale", before.defaultLocale(), updated.defaultLocale());
        addIfChanged(fields, "defaultCurrency", before.defaultCurrency(), updated.defaultCurrency());
        addIfChanged(fields, "timeZone", before.timeZone(), updated.timeZone());
        addIfChanged(fields, "logoUrl", before.logoUrl(), updated.logoUrl());
        addIfChanged(fields, "faviconUrl", before.faviconUrl(), updated.faviconUrl());
        addIfChanged(fields, "loginImageUrl", before.loginImageUrl(), updated.loginImageUrl());
        addIfChanged(fields, "primaryColor", before.primaryColor(), updated.primaryColor());
        addIfChanged(fields, "secondaryColor", before.secondaryColor(), updated.secondaryColor());
        addIfChanged(fields, "themeMode", before.themeMode(), updated.themeMode());
        addIfChanged(fields, "dateFormat", before.dateFormat(), updated.dateFormat());
        addIfChanged(fields, "timeFormat", before.timeFormat(), updated.timeFormat());
        addIfChanged(fields, "numberLocale", before.numberLocale(), updated.numberLocale());
        addIfChanged(fields, "currencyFormat", before.currencyFormat(), updated.currencyFormat());
        return fields;
    }

    private void addIfChanged(Map<String, Object> fields, String field, Object before, Object after) {
        if (!java.util.Objects.equals(before, after)) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("before", before);
            values.put("after", after);
            fields.put(field, values);
        }
    }
}