package com.koda.platform.shared.api;

import com.koda.platform.platform.configuration.application.CompanySettingsNotFoundException;
import com.koda.platform.platform.configuration.application.CompanySettingsVersionConflictException;
import com.koda.platform.platform.security.application.AuthenticationFailedException;
import com.koda.platform.platform.security.application.InvalidRefreshTokenException;
import com.koda.platform.platform.security.application.TenantSelectionRequiredException;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.MissingTenantContextException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        problem.setTitle("Invalid request");
        problem.setProperty("code", "MALFORMED_REQUEST_BODY");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler({AuthenticationFailedException.class, InvalidRefreshTokenException.class})
    ProblemDetail handleAuthenticationFailure(RuntimeException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Authentication failed");
        problem.setProperty("code", exception instanceof InvalidRefreshTokenException ? "INVALID_REFRESH_TOKEN" : "AUTHENTICATION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(TenantSelectionRequiredException.class)
    ProblemDetail handleTenantSelectionRequired(TenantSelectionRequiredException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Tenant selection required");
        problem.setProperty("code", "TENANT_SELECTION_REQUIRED");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(PermissionDeniedException.class)
    ProblemDetail handlePermissionDenied(PermissionDeniedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Permission denied");
        problem.setProperty("code", "PERMISSION_DENIED");
        problem.setProperty("requiredPermission", exception.requiredPermission());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CompanySettingsNotFoundException.class)
    ProblemDetail handleCompanySettingsNotFound(CompanySettingsNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Company settings not found");
        problem.setProperty("code", "COMPANY_SETTINGS_NOT_FOUND");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CompanySettingsVersionConflictException.class)
    ProblemDetail handleCompanySettingsVersionConflict(CompanySettingsVersionConflictException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Company settings version conflict");
        problem.setProperty("code", "COMPANY_SETTINGS_VERSION_CONFLICT");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
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

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
        problem.setTitle("Invalid request");
        problem.setProperty("code", "INVALID_REQUEST");
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
