package com.koda.platform;

import com.koda.platform.platform.security.application.AuthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "koda.security.jwt.secret=test-secret-test-secret-test-secret-32",
    "koda.security.auth.jdbc.enabled=false"
})
class KodaPlatformApplicationTests {

    @MockitoBean
    private AuthRepository authRepository;

    @Test
    void contextLoads() {
    }
}
