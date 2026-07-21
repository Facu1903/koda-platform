package com.koda.platform;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.koda.platform.platform.audit.application.AuditRepository;
import com.koda.platform.platform.cash.application.CashRepository;
import com.koda.platform.platform.catalog.application.CatalogRepository;
import com.koda.platform.platform.commercial.application.CommercialPartnerRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
}
