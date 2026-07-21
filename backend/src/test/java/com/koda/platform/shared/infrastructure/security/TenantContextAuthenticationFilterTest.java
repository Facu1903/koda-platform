package com.koda.platform.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.koda.platform.shared.domain.tenant.TenantId;
import com.koda.platform.shared.infrastructure.observability.LoggingContextKeys;
import com.koda.platform.shared.infrastructure.tenant.TenantContextHolder;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TenantContextAuthenticationFilterTest {

    private final TenantContextAuthenticationFilter filter = new TenantContextAuthenticationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        MDC.clear();
    }

    @Test
    void setsTenantContextFromKodaPrincipalDuringRequestAndClearsItAfterwards() throws ServletException, IOException {
        TenantId tenantId = TenantId.from(UUID.randomUUID());
        UUID userId = UUID.randomUUID();
        KodaAuthenticatedPrincipal principal = new KodaAuthenticatedPrincipal(
            userId,
            tenantId,
            "owner@koda.local",
            Set.of("TENANT_OWNER"),
            Set.of("products:read"),
            false
        );
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null, "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            chainInvoked.set(true);
            assertThat(TenantContextHolder.get()).hasValueSatisfying(context -> {
                assertThat(context.tenantId()).isEqualTo(tenantId);
                assertThat(context.userId()).isEqualTo(userId);
                assertThat(context.hasRole("TENANT_OWNER")).isTrue();
                assertThat(context.hasPermission("products:read")).isTrue();
            });
            assertThat(MDC.get(LoggingContextKeys.TENANT_ID)).isEqualTo(tenantId.toString());
            assertThat(MDC.get(LoggingContextKeys.USER_ID)).isEqualTo(userId.toString());
            assertThat(MDC.get(LoggingContextKeys.PLATFORM_ADMIN)).isEqualTo("false");
            assertThat(servletRequest.getAttribute(LoggingContextKeys.TENANT_ID_REQUEST_ATTRIBUTE)).isEqualTo(tenantId.toString());
            assertThat(servletRequest.getAttribute(LoggingContextKeys.USER_ID_REQUEST_ATTRIBUTE)).isEqualTo(userId.toString());
            assertThat(servletRequest.getAttribute(LoggingContextKeys.PLATFORM_ADMIN_REQUEST_ATTRIBUTE)).isEqualTo("false");
        });

        assertThat(chainInvoked).isTrue();
        assertThat(TenantContextHolder.get()).isEmpty();
        assertThat(MDC.get(LoggingContextKeys.TENANT_ID)).isNull();
        assertThat(MDC.get(LoggingContextKeys.USER_ID)).isNull();
        assertThat(MDC.get(LoggingContextKeys.PLATFORM_ADMIN)).isNull();
    }

    @Test
    void rejectsAuthenticatedTenantScopedApiRequestWithoutKodaSecurityPrincipal() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("basic-user", null, "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(chainInvoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void allowsTenantNeutralActuatorPathWithoutKodaSecurityPrincipal() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("basic-user", null, "ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(chainInvoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    void allowsPlatformApiPathWithoutKodaSecurityPrincipal() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("platform-user", null, "ROLE_PLATFORM"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/tenants");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

        assertThat(chainInvoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.get()).isEmpty();
    }
}
