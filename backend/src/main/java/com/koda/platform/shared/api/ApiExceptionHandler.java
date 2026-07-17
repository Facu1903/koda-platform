package com.koda.platform.shared.api;

import com.koda.platform.shared.application.tenant.MissingTenantContextException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation error");
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("details", exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
            .toList());
        return problem;
    }

    @ExceptionHandler(MissingTenantContextException.class)
    ProblemDetail handleMissingTenantContext(MissingTenantContextException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Tenant context required");
        problem.setProperty("code", "TENANT_CONTEXT_REQUIRED");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error");
        problem.setTitle("Internal server error");
        problem.setProperty("code", "INTERNAL_ERROR");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    private record FieldValidationError(String field, String message) {
    }
}