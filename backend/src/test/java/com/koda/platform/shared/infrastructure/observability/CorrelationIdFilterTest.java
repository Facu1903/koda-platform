package com.koda.platform.shared.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class CorrelationIdFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void reusesValidIncomingCorrelationIdAndClearsMdcAfterRequest() throws ServletException, IOException {
        CorrelationIdFilter filter = new CorrelationIdFilter(() -> "generated-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/123");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "external-trace-123");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/products/{id}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            chainInvoked.set(true);
            assertThat(MDC.get(LoggingContextKeys.CORRELATION_ID)).isEqualTo("external-trace-123");
            assertThat(MDC.get(LoggingContextKeys.HTTP_METHOD)).isEqualTo("GET");
        });

        assertThat(chainInvoked).isTrue();
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("external-trace-123");
        assertThat(MDC.get(LoggingContextKeys.CORRELATION_ID)).isNull();
        assertThat(MDC.get(LoggingContextKeys.HTTP_METHOD)).isNull();
        assertThat(MDC.get(LoggingContextKeys.HTTP_PATH)).isNull();
        assertThat(MDC.get(LoggingContextKeys.HTTP_STATUS)).isNull();
        assertThat(MDC.get(LoggingContextKeys.HTTP_DURATION_MS)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        CorrelationIdFilter filter = new CorrelationIdFilter(() -> "generated-id");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/sales");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
            assertThat(MDC.get(LoggingContextKeys.CORRELATION_ID)).isEqualTo("generated-id")
        );

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("generated-id");
        assertThat(MDC.get(LoggingContextKeys.CORRELATION_ID)).isNull();
    }

    @Test
    void rejectsUnsafeCorrelationIdAndUsesGeneratedOne() throws ServletException, IOException {
        CorrelationIdFilter filter = new CorrelationIdFilter(() -> "generated-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/reports/dashboard");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "bad id with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
            assertThat(MDC.get(LoggingContextKeys.CORRELATION_ID)).isEqualTo("generated-id")
        );

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("generated-id");
    }

    @Test
    void requestCompletionLogContainsRequestAndTenantContextFields() throws ServletException, IOException {
        CorrelationIdFilter filter = new CorrelationIdFilter(() -> "generated-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products/123");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/products/{id}");
        request.setAttribute(LoggingContextKeys.TENANT_ID_REQUEST_ATTRIBUTE, "tenant-1");
        request.setAttribute(LoggingContextKeys.USER_ID_REQUEST_ATTRIBUTE, "user-1");
        request.setAttribute(LoggingContextKeys.PLATFORM_ADMIN_REQUEST_ATTRIBUTE, "false");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ListAppender<ILoggingEvent> appender = attachListAppender();

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            });
        } finally {
            detachListAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).isEqualTo("http.request.completed");
        assertThat(appender.list.get(0).getMDCPropertyMap())
            .containsEntry(LoggingContextKeys.CORRELATION_ID, "generated-id")
            .containsEntry(LoggingContextKeys.HTTP_METHOD, "GET")
            .containsEntry(LoggingContextKeys.HTTP_PATH, "/api/v1/products/{id}")
            .containsEntry(LoggingContextKeys.HTTP_STATUS, "200")
            .containsEntry(LoggingContextKeys.TENANT_ID, "tenant-1")
            .containsEntry(LoggingContextKeys.USER_ID, "user-1")
            .containsEntry(LoggingContextKeys.PLATFORM_ADMIN, "false")
            .containsKey(LoggingContextKeys.HTTP_DURATION_MS);
        assertThat(MDC.get(LoggingContextKeys.TENANT_ID)).isNull();
    }

    @Test
    void logsServerErrorStatusWhenRequestFailsBeforeResponseStatusIsSet() {
        CorrelationIdFilter filter = new CorrelationIdFilter(() -> "generated-id");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ListAppender<ILoggingEvent> appender = attachListAppender();

        try {
            assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                throw new ServletException("boom");
            }))
                .isInstanceOf(ServletException.class)
                .hasMessage("boom");
        } finally {
            detachListAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getMDCPropertyMap())
            .containsEntry(LoggingContextKeys.CORRELATION_ID, "generated-id")
            .containsEntry(LoggingContextKeys.HTTP_STATUS, "500");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("generated-id");
        assertThat(MDC.get(LoggingContextKeys.CORRELATION_ID)).isNull();
    }

    private ListAppender<ILoggingEvent> attachListAppender() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachListAppender(ListAppender<ILoggingEvent> appender) {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        logger.detachAppender(appender);
    }
}
