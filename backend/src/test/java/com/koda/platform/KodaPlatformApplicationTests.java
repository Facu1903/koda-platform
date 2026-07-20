package com.koda.platform;

import com.koda.platform.platform.audit.application.AuditRepository;
import com.koda.platform.platform.cash.application.CashRepository;
import com.koda.platform.platform.catalog.application.CatalogRepository;
import com.koda.platform.platform.commercial.application.CommercialPartnerRepository;
import com.koda.platform.platform.configuration.application.CompanySettingsRepository;
import com.koda.platform.platform.licensing.application.TenantCapabilitiesRepository;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessRepository;
import com.koda.platform.platform.security.application.AuthRepository;
import com.koda.platform.platform.purchases.application.PurchasesCashPort;
import com.koda.platform.platform.purchases.application.PurchasesRepository;
import com.koda.platform.platform.purchases.application.PurchasesStockPort;
import com.koda.platform.platform.reports.application.ReportsRepository;
import com.koda.platform.platform.sales.application.SalesCashPort;
import com.koda.platform.platform.sales.application.SalesRepository;
import com.koda.platform.platform.sales.application.SalesStockPort;
import com.koda.platform.platform.stock.application.StockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
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
class KodaPlatformApplicationTests {

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

    @Test
    void contextLoads() {
    }
}
