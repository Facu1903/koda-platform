package com.koda.platform.shared.api;

import com.koda.platform.platform.catalog.application.CatalogItemNotFoundException;
import com.koda.platform.platform.catalog.application.CatalogReferenceNotFoundException;
import com.koda.platform.platform.catalog.application.CatalogVersionConflictException;
import com.koda.platform.platform.cash.application.CashItemNotFoundException;
import com.koda.platform.platform.cash.application.CashOperationRejectedException;
import com.koda.platform.platform.cash.application.CashVersionConflictException;
import com.koda.platform.platform.commercial.application.CommercialPartnerNotFoundException;
import com.koda.platform.platform.commercial.application.CommercialPartnerOperationRejectedException;
import com.koda.platform.platform.commercial.application.CommercialPartnerVersionConflictException;
import com.koda.platform.platform.configuration.application.CompanySettingsNotFoundException;
import com.koda.platform.platform.stock.application.StockItemNotFoundException;
import com.koda.platform.platform.stock.application.StockMovementRejectedException;
import com.koda.platform.platform.stock.application.StockReferenceNotFoundException;
import com.koda.platform.platform.configuration.application.CompanySettingsVersionConflictException;
import com.koda.platform.platform.security.application.AuthenticationFailedException;
import com.koda.platform.platform.security.application.InvalidRefreshTokenException;
import com.koda.platform.platform.security.application.TenantSelectionRequiredException;
import com.koda.platform.platform.purchases.application.PurchaseNotFoundException;
import com.koda.platform.platform.purchases.application.PurchaseOperationRejectedException;
import com.koda.platform.platform.purchases.application.PurchaseReferenceNotFoundException;
import com.koda.platform.platform.purchases.application.PurchaseVersionConflictException;
import com.koda.platform.platform.sales.application.SaleNotFoundException;
import com.koda.platform.platform.sales.application.SaleOperationRejectedException;
import com.koda.platform.platform.sales.application.SaleReferenceNotFoundException;
import com.koda.platform.platform.sales.application.SaleVersionConflictException;
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


    @ExceptionHandler(CatalogItemNotFoundException.class)
    ProblemDetail handleCatalogItemNotFound(CatalogItemNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Catalog item not found");
        problem.setProperty("code", "CATALOG_ITEM_NOT_FOUND");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CatalogReferenceNotFoundException.class)
    ProblemDetail handleCatalogReferenceNotFound(CatalogReferenceNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Catalog reference not found");
        problem.setProperty("code", "CATALOG_REFERENCE_NOT_FOUND");
        problem.setProperty("reference", exception.reference());
        problem.setProperty("referenceId", exception.referenceId());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CatalogVersionConflictException.class)
    ProblemDetail handleCatalogVersionConflict(CatalogVersionConflictException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Catalog version conflict");
        problem.setProperty("code", "CATALOG_VERSION_CONFLICT");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }


    @ExceptionHandler(CommercialPartnerNotFoundException.class)
    ProblemDetail handleCommercialPartnerNotFound(CommercialPartnerNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Commercial partner not found");
        problem.setProperty("code", "COMMERCIAL_PARTNER_NOT_FOUND");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CommercialPartnerVersionConflictException.class)
    ProblemDetail handleCommercialPartnerVersionConflict(CommercialPartnerVersionConflictException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Commercial partner version conflict");
        problem.setProperty("code", "COMMERCIAL_PARTNER_VERSION_CONFLICT");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CommercialPartnerOperationRejectedException.class)
    ProblemDetail handleCommercialPartnerOperationRejected(CommercialPartnerOperationRejectedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Commercial partner operation rejected");
        problem.setProperty("code", "COMMERCIAL_PARTNER_OPERATION_REJECTED");
        problem.setProperty("reasonCode", exception.reasonCode());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }



    @ExceptionHandler(PurchaseNotFoundException.class)
    ProblemDetail handlePurchaseNotFound(PurchaseNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Purchase not found");
        problem.setProperty("code", "PURCHASE_NOT_FOUND");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(PurchaseReferenceNotFoundException.class)
    ProblemDetail handlePurchaseReferenceNotFound(PurchaseReferenceNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Purchase reference not found");
        problem.setProperty("code", "PURCHASE_REFERENCE_NOT_FOUND");
        problem.setProperty("reference", exception.reference());
        problem.setProperty("referenceId", exception.referenceId());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(PurchaseVersionConflictException.class)
    ProblemDetail handlePurchaseVersionConflict(PurchaseVersionConflictException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Purchase version conflict");
        problem.setProperty("code", "PURCHASE_VERSION_CONFLICT");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(PurchaseOperationRejectedException.class)
    ProblemDetail handlePurchaseOperationRejected(PurchaseOperationRejectedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Purchase operation rejected");
        problem.setProperty("code", "PURCHASE_OPERATION_REJECTED");
        problem.setProperty("reasonCode", exception.reasonCode());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }
    @ExceptionHandler(SaleNotFoundException.class)
    ProblemDetail handleSaleNotFound(SaleNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Sale not found");
        problem.setProperty("code", "SALE_NOT_FOUND");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(SaleReferenceNotFoundException.class)
    ProblemDetail handleSaleReferenceNotFound(SaleReferenceNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Sale reference not found");
        problem.setProperty("code", "SALE_REFERENCE_NOT_FOUND");
        problem.setProperty("reference", exception.reference());
        problem.setProperty("referenceId", exception.referenceId());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(SaleVersionConflictException.class)
    ProblemDetail handleSaleVersionConflict(SaleVersionConflictException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Sale version conflict");
        problem.setProperty("code", "SALE_VERSION_CONFLICT");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(SaleOperationRejectedException.class)
    ProblemDetail handleSaleOperationRejected(SaleOperationRejectedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Sale operation rejected");
        problem.setProperty("code", "SALE_OPERATION_REJECTED");
        problem.setProperty("reasonCode", exception.reasonCode());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }
    @ExceptionHandler(CashItemNotFoundException.class)
    ProblemDetail handleCashItemNotFound(CashItemNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Cash item not found");
        problem.setProperty("code", "CASH_ITEM_NOT_FOUND");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CashVersionConflictException.class)
    ProblemDetail handleCashVersionConflict(CashVersionConflictException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cash version conflict");
        problem.setProperty("code", "CASH_VERSION_CONFLICT");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(CashOperationRejectedException.class)
    ProblemDetail handleCashOperationRejected(CashOperationRejectedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cash operation rejected");
        problem.setProperty("code", "CASH_OPERATION_REJECTED");
        problem.setProperty("reasonCode", exception.reasonCode());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }
    @ExceptionHandler(StockItemNotFoundException.class)
    ProblemDetail handleStockItemNotFound(StockItemNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Stock item not found");
        problem.setProperty("code", "STOCK_ITEM_NOT_FOUND");
        problem.setProperty("resource", exception.resource());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(StockReferenceNotFoundException.class)
    ProblemDetail handleStockReferenceNotFound(StockReferenceNotFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Stock reference not found");
        problem.setProperty("code", "STOCK_REFERENCE_NOT_FOUND");
        problem.setProperty("reference", exception.reference());
        problem.setProperty("referenceId", exception.referenceId());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    @ExceptionHandler(StockMovementRejectedException.class)
    ProblemDetail handleStockMovementRejected(StockMovementRejectedException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Stock movement rejected");
        problem.setProperty("code", "STOCK_MOVEMENT_REJECTED");
        problem.setProperty("reasonCode", exception.reasonCode());
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
