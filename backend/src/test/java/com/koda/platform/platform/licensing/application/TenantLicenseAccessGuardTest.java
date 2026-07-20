package com.koda.platform.platform.licensing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantLicenseAccessGuardTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-20T18:30:00Z");

    private final TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private final UUID userId = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private FakeTenantLicenseAccessRepository repository;
    private TenantLicenseAccessGuard guard;

    @BeforeEach
    void setUp() {
        repository = new FakeTenantLicenseAccessRepository();
        guard = new TenantLicenseAccessGuard(repository, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void allowsEnabledProductAndModule() {
        assertThatCode(() -> guard.requireModuleEnabled(context(false), LicensedProducts.KODA_ERP, LicensedModules.SALES))
            .doesNotThrowAnyException();

        assertThat(repository.lastCalculatedAt).isEqualTo(FIXED_NOW);
    }

    @Test
    void disabledProductBlocksBeforeCheckingModule() {
        repository.productEnabled = false;

        assertThatThrownBy(() -> guard.requireModuleEnabled(context(false), LicensedProducts.KODA_ERP, LicensedModules.SALES))
            .isInstanceOf(TenantLicenseAccessDeniedException.class)
            .satisfies(exception -> {
                TenantLicenseAccessDeniedException denied = (TenantLicenseAccessDeniedException) exception;
                assertThat(denied.reasonCode()).isEqualTo("PRODUCT_NOT_ENABLED");
                assertThat(denied.productCode()).isEqualTo(LicensedProducts.KODA_ERP);
                assertThat(denied.moduleCode()).isEqualTo(LicensedModules.SALES);
            });
        assertThat(repository.moduleChecks).isZero();
    }

    @Test
    void disabledModuleBlocksTenantOperation() {
        repository.moduleEnabled = false;

        assertThatThrownBy(() -> guard.requireModuleEnabled(context(false), LicensedProducts.KODA_ERP, LicensedModules.SALES))
            .isInstanceOf(TenantLicenseAccessDeniedException.class)
            .satisfies(exception -> {
                TenantLicenseAccessDeniedException denied = (TenantLicenseAccessDeniedException) exception;
                assertThat(denied.reasonCode()).isEqualTo("MODULE_NOT_ENABLED");
                assertThat(denied.productCode()).isEqualTo(LicensedProducts.KODA_ERP);
                assertThat(denied.moduleCode()).isEqualTo(LicensedModules.SALES);
            });
    }

    @Test
    void platformAdminDoesNotBypassTenantLicense() {
        repository.moduleEnabled = false;

        assertThatThrownBy(() -> guard.requireModuleEnabled(context(true), LicensedProducts.KODA_ERP, LicensedModules.SALES))
            .isInstanceOf(TenantLicenseAccessDeniedException.class)
            .satisfies(exception -> assertThat(((TenantLicenseAccessDeniedException) exception).reasonCode()).isEqualTo("MODULE_NOT_ENABLED"));
    }

    private TenantContext context(boolean platformAdmin) {
        return new TenantContext(tenantId, userId, Set.of("TENANT_ADMIN"), Set.of("sales:read"), platformAdmin);
    }

    private static final class FakeTenantLicenseAccessRepository implements TenantLicenseAccessRepository {
        private boolean productEnabled = true;
        private boolean moduleEnabled = true;
        private int moduleChecks;
        private Instant lastCalculatedAt;

        @Override
        public boolean isProductEnabled(TenantId tenantId, String productCode, Instant calculatedAt) {
            lastCalculatedAt = calculatedAt;
            return productEnabled;
        }

        @Override
        public boolean isModuleEnabled(TenantId tenantId, String productCode, String moduleCode, Instant calculatedAt) {
            moduleChecks++;
            lastCalculatedAt = calculatedAt;
            return moduleEnabled;
        }
    }
}
