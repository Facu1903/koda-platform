package com.koda.platform.shared.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    static final int MIN_CORRELATION_ID_LENGTH = 8;
    static final int MAX_CORRELATION_ID_LENGTH = 128;

    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]*$");

    private final Supplier<String> correlationIdSupplier;

    public CorrelationIdFilter() {
        this(() -> UUID.randomUUID().toString());
    }

    CorrelationIdFilter(Supplier<String> correlationIdSupplier) {
        this.correlationIdSupplier = correlationIdSupplier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        long startedAt = System.nanoTime();
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        String httpMethod = request.getMethod();

        MDC.put(LoggingContextKeys.CORRELATION_ID, correlationId);
        MDC.put(LoggingContextKeys.HTTP_METHOD, httpMethod);
        response.setHeader(HEADER_NAME, correlationId);

        Throwable failure = null;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            String httpPath = normalizedPath(request);
            int status = resolveStatus(response, failure);

            MDC.put(LoggingContextKeys.HTTP_PATH, httpPath);
            MDC.put(LoggingContextKeys.HTTP_STATUS, Integer.toString(status));
            MDC.put(LoggingContextKeys.HTTP_DURATION_MS, Long.toString(durationMs));
            putIfPresent(LoggingContextKeys.TENANT_ID, attributeAsString(request, LoggingContextKeys.TENANT_ID_REQUEST_ATTRIBUTE));
            putIfPresent(LoggingContextKeys.USER_ID, attributeAsString(request, LoggingContextKeys.USER_ID_REQUEST_ATTRIBUTE));
            putIfPresent(LoggingContextKeys.PLATFORM_ADMIN, attributeAsString(request, LoggingContextKeys.PLATFORM_ADMIN_REQUEST_ATTRIBUTE));

            LOGGER.info("http.request.completed");

            MDC.remove(LoggingContextKeys.PLATFORM_ADMIN);
            MDC.remove(LoggingContextKeys.USER_ID);
            MDC.remove(LoggingContextKeys.TENANT_ID);
            MDC.remove(LoggingContextKeys.HTTP_DURATION_MS);
            MDC.remove(LoggingContextKeys.HTTP_STATUS);
            MDC.remove(LoggingContextKeys.HTTP_PATH);
            MDC.remove(LoggingContextKeys.HTTP_METHOD);
            MDC.remove(LoggingContextKeys.CORRELATION_ID);
        }
    }

    private String resolveCorrelationId(String rawCorrelationId) {
        if (rawCorrelationId == null) {
            return correlationIdSupplier.get();
        }

        String candidate = rawCorrelationId.trim();
        if (candidate.length() < MIN_CORRELATION_ID_LENGTH || candidate.length() > MAX_CORRELATION_ID_LENGTH) {
            return correlationIdSupplier.get();
        }

        if (!SAFE_CORRELATION_ID.matcher(candidate).matches()) {
            return correlationIdSupplier.get();
        }

        return candidate;
    }

    private String normalizedPath(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        String requestUri = request.getRequestURI();
        return requestUri == null || requestUri.isBlank() ? "/" : requestUri;
    }

    private int resolveStatus(HttpServletResponse response, Throwable failure) {
        int status = response.getStatus();
        if (failure != null && status < HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        return status;
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private String attributeAsString(HttpServletRequest request, String attributeName) {
        Object value = request.getAttribute(attributeName);
        return value == null ? null : value.toString();
    }
}
