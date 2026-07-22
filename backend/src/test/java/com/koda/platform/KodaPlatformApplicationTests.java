package com.koda.platform;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.koda.platform.platform.audit.application.AuditRepository;
import com.koda.platform.platform.cash.application.CashRepository;
import com.koda.platform.platform.catalog.application.CatalogRepository;
import com.koda.platform.platform.commercial.application.CommercialPartnerRepository;
import com.koda.platform.platform.configuration.application.CompanySettings;
import com.koda.platform.platform.configuration.application.CompanySettingsRepository;
import com.koda.platform.platform.licensing.application.TenantCapabilitiesRepository;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessRepository;
import com.koda.platform.platform.licensing.application.TenantLicenseAdministrationRepository;
import com.koda.platform.platform.security.application.AuthRepository;
import com.koda.platform.platform.purchases.application.PurchasesCashPort;
import com.koda.platform.platform.purchases.application.PurchasesRepository;
import com.koda.platform.platform.purchases.application.PurchasesStockPort;
import com.koda.platform.platform.reports.application.ReportsRepository;
import com.koda.platform.platform.sales.application.SalesCashPort;
import com.koda.platform.platform.sales.application.SalesRepository;
import com.koda.platform.platform.sales.application.SalesStockPort;
import com.koda.platform.platform.stock.application.StockRepository;
import com.koda.platform.shared.domain.tenant.TenantId;
import com.koda.platform.shared.infrastructure.security.KodaAuthenticatedPrincipal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "spring.flyway.enabled=false",
    "management.endpoint.health.validate-group-membership=false",
    "koda.security.jwt.secret=test-secret-test-secret-test-secret-32",
    "koda.security.auth.jdbc.enabled=false",
    "koda.configuration.company-settings.jdbc.enabled=false",
    "koda.catalogs.jdbc.enabled=false",
    "koda.stock.jdbc.enabled=false",
    "koda.audit.jdbc.enabled=false",
    "koda.commercial.jdbc.enabled=false",
    "koda.cash.jdbc.enabled=false",
    "koda.sales.jdbc.enabled=false",
    "koda.purchases.jdbc.enabled=false",
    "koda.reports.jdbc.enabled=false",
    "koda.licensing.jdbc.enabled=false"
})
@AutoConfigureMockMvc
class KodaPlatformApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthRepository authRepository;

    @MockitoBean
    private CompanySettingsRepository companySettingsRepository;

    @MockitoBean
    private CatalogRepository catalogRepository;

    @MockitoBean
    private StockRepository stockRepository;

    @MockitoBean
    private AuditRepository auditRepository;

    @MockitoBean
    private CommercialPartnerRepository commercialPartnerRepository;

    @MockitoBean
    private CashRepository cashRepository;

    @MockitoBean
    private SalesRepository salesRepository;

    @MockitoBean
    private SalesStockPort salesStockPort;

    @MockitoBean
    private SalesCashPort salesCashPort;

    @MockitoBean
    private PurchasesRepository purchasesRepository;

    @MockitoBean
    private PurchasesStockPort purchasesStockPort;

    @MockitoBean
    private PurchasesCashPort purchasesCashPort;

    @MockitoBean
    private ReportsRepository reportsRepository;

    @MockitoBean
    private TenantCapabilitiesRepository tenantCapabilitiesRepository;

    @MockitoBean
    private TenantLicenseAccessRepository tenantLicenseAccessRepository;

    @MockitoBean
    private TenantLicenseAdministrationRepository tenantLicenseAdministrationRepository;

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Actuator health subpaths are public and do not expose details")
    void actuatorHealthSubpathsArePublicAndDoNotExposeDetails() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.livenessState.status").value("UP"))
            .andExpect(jsonPath("$.components.livenessState.details").doesNotExist());

        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.readinessState.status").value("UP"))
            .andExpect(jsonPath("$.components.readinessState.details").doesNotExist());
    }

    @Test
    @DisplayName("Actuator metrics are exposed only to authenticated operators")
    void actuatorMetricsAreExposedOnlyToAuthenticatedOperators() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics").with(user("ops")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("HTTP server request metrics are available with stable tags")
    void httpServerRequestMetricsAreAvailableWithStableTags() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/http.server.requests").with(user("ops")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("http.server.requests"))
            .andExpect(jsonPath("$.availableTags[?(@.tag == 'uri')]").exists())
            .andExpect(jsonPath("$.availableTags[?(@.tag == 'tenantId')]").doesNotExist())
            .andExpect(jsonPath("$.availableTags[?(@.tag == 'userId')]").doesNotExist())
            .andExpect(jsonPath("$.availableTags[?(@.tag == 'correlationId')]").doesNotExist());
    }

    @Test
    @DisplayName("Company runtime profile is tenant scoped and omits administrative fields")
    void companyRuntimeProfileIsTenantScopedAndOmitsAdministrativeFields() throws Exception {
        TenantId tenantId = TenantId.from(UUID.fromString("00000000-0000-4000-8000-000000000001"));
        when(tenantLicenseAccessRepository.isProductEnabled(eq(tenantId), eq("KODA_ERP"), any())).thenReturn(true);
        when(tenantLicenseAccessRepository.isModuleEnabled(eq(tenantId), eq("KODA_ERP"), eq("CONFIGURATION"), any())).thenReturn(true);
        when(companySettingsRepository.findByTenantId(tenantId)).thenReturn(Optional.of(new CompanySettings(
            UUID.fromString("00000000-0000-4000-8000-000000000002"),
            tenantId,
            "KODA",
            "KODA Legal Name",
            "AR",
            "es-AR",
            "ARS",
            "America/Argentina/Buenos_Aires",
            "https://cdn.koda.local/logo.png",
            null,
            null,
            "#F6862B",
            null,
            "dark",
            "dd/MM/yyyy",
            "HH:mm",
            "es-AR",
            "symbol",
            7L,
            Instant.parse("2026-07-22T12:00:00Z")
        )));
        KodaAuthenticatedPrincipal principal = new KodaAuthenticatedPrincipal(
            UUID.fromString("40000000-0000-4000-8000-000000000001"),
            tenantId,
            "sales@koda.local",
            Set.of("SALES_USER"),
            Set.of(),
            false
        );

        mockMvc.perform(get("/api/v1/company/profile").with(authentication(new TestingAuthenticationToken(principal, null, "ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenant.id").value(tenantId.toString()))
            .andExpect(jsonPath("$.tenant.commercialName").value("KODA"))
            .andExpect(jsonPath("$.tenant.countryCode").value("AR"))
            .andExpect(jsonPath("$.tenant.legalName").doesNotExist())
            .andExpect(jsonPath("$.branding.logoUrl").value("https://cdn.koda.local/logo.png"))
            .andExpect(jsonPath("$.branding.primaryColor").value("#F6862B"))
            .andExpect(jsonPath("$.regional.defaultLocale").value("es-AR"))
            .andExpect(jsonPath("$.regional.defaultCurrency").value("ARS"))
            .andExpect(jsonPath("$.version").doesNotExist());
    }
}
