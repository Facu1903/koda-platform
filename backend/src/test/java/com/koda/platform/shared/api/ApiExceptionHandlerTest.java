package com.koda.platform.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void missingRequestParameterReturnsStructuredBadRequest() {
        MockHttpServletRequest request = request("/api/v1/reports/sales");

        ProblemDetail problem = handler.handleMissingRequestParameter(new MissingServletRequestParameterException("from", "Instant"), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "MISSING_REQUEST_PARAMETER");
        assertThat(problem.getProperties()).containsEntry("parameter", "from");
        assertThat(problem.getProperties()).containsEntry("path", "/api/v1/reports/sales");
    }

    @Test
    void invalidRequestParameterReturnsStructuredBadRequest() {
        MockHttpServletRequest request = request("/api/v1/stock/movements/not-a-uuid");
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException("not-a-uuid", java.util.UUID.class, "id", null,
            new IllegalArgumentException("Invalid UUID"));

        ProblemDetail problem = handler.handleMethodArgumentTypeMismatch(exception, request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_REQUEST_PARAMETER");
        assertThat(problem.getProperties()).containsEntry("parameter", "id");
    }

    @Test
    void queryParameterValidationReturnsStructuredBadRequest() {
        MockHttpServletRequest request = request("/api/v1/reports/dashboard");

        ProblemDetail problem = handler.handleRequestParameterValidation(new ConstraintViolationException("Limit invalid", Set.of()), request);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION_ERROR");
        assertThat(problem.getProperties()).containsEntry("path", "/api/v1/reports/dashboard");
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}