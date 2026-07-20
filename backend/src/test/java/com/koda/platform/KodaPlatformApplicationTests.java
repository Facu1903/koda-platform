package com.koda.platform;

import com.koda.platform.platform.audit.application.AuditRepository;
import com.koda.platform.platform.catalog.application.CatalogRepository;
import com.koda.platform.platform.commercial.application.CommercialPartnerRepository;
import com.koda.platform.platform.configuration.application.CompanySettingsRepository;
import com.koda.platform.platform.security.application.AuthRepository;
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
    "koda.commercial.jdbc.enabled=false"
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

    @Test
    void contextLoads() {
    }
}
