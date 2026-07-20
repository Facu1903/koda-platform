package com.koda.platform.platform.sales.api;

import com.koda.platform.platform.sales.application.CancelSaleCommand;
import com.koda.platform.platform.sales.application.ConfirmSaleCommand;
import com.koda.platform.platform.sales.application.CreateSaleCommand;
import com.koda.platform.platform.sales.application.Sale;
import com.koda.platform.platform.sales.application.SaleItem;
import com.koda.platform.platform.sales.application.SaleItemCommand;
import com.koda.platform.platform.sales.application.SalesRequestMetadata;
import com.koda.platform.platform.sales.application.SalesService;
import com.koda.platform.platform.sales.application.UpdateSaleCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping
    public List<SaleResponse> listSales(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return salesService.listSales(limit).stream().map(SaleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public SaleResponse getSale(@PathVariable UUID id) {
        return SaleResponse.from(salesService.getSale(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse createSale(@Valid @RequestBody SaleRequest request, HttpServletRequest httpRequest) {
        return SaleResponse.from(salesService.createSale(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/{id}")
    public SaleResponse updateSale(@PathVariable UUID id, @Valid @RequestBody VersionedSaleRequest request, HttpServletRequest httpRequest) {
        return SaleResponse.from(salesService.updateSale(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSale(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        salesService.deleteSale(id, version, metadata(httpRequest));
    }

    @PostMapping("/{id}/confirm")
    public SaleResponse confirmSale(@PathVariable UUID id, @Valid @RequestBody ConfirmSaleRequest request, HttpServletRequest httpRequest) {
        return SaleResponse.from(salesService.confirmSale(id, request.toCommand(), metadata(httpRequest)));
    }

    @PostMapping("/{id}/cancel")
    public SaleResponse cancelSale(@PathVariable UUID id, @Valid @RequestBody CancelSaleRequest request, HttpServletRequest httpRequest) {
        return SaleResponse.from(salesService.cancelSale(id, request.toCommand(), metadata(httpRequest)));
    }

    private SalesRequestMetadata metadata(HttpServletRequest request) {
        return new SalesRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record SaleRequest(
        @NotNull UUID branchId,
        UUID customerId,
        @Size(min = 1, max = 200) List<@Valid SaleItemRequest> items
    ) {
        CreateSaleCommand toCreateCommand() {
            return new CreateSaleCommand(branchId, customerId, items == null ? null : items.stream().map(SaleItemRequest::toCommand).toList());
        }
    }

    public record VersionedSaleRequest(
        @Min(0) long version,
        @NotNull UUID branchId,
        UUID customerId,
        @Size(min = 1, max = 200) List<@Valid SaleItemRequest> items
    ) {
        UpdateSaleCommand toUpdateCommand() {
            return new UpdateSaleCommand(version, branchId, customerId, items == null ? null : items.stream().map(SaleItemRequest::toCommand).toList());
        }
    }

    public record SaleItemRequest(
        @NotNull UUID productId,
        UUID warehouseId,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0000", inclusive = true) BigDecimal unitPrice
    ) {
        SaleItemCommand toCommand() {
            return new SaleItemCommand(productId, warehouseId, quantity, unitPrice);
        }
    }

    public record ConfirmSaleRequest(
        @Min(0) long version,
        UUID cashSessionId,
        @Size(max = 32) String paymentMethod
    ) {
        ConfirmSaleCommand toCommand() {
            return new ConfirmSaleCommand(version, cashSessionId, paymentMethod);
        }
    }

    public record CancelSaleRequest(
        @Min(0) long version,
        @Size(max = 500) String reason,
        UUID cashSessionId
    ) {
        CancelSaleCommand toCommand() {
            return new CancelSaleCommand(version, reason, cashSessionId);
        }
    }

    public record SaleResponse(
        UUID id,
        UUID branchId,
        UUID customerId,
        long saleNumber,
        String numberCode,
        String status,
        String currencyCode,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String paymentMethod,
        BigDecimal paidAmount,
        UUID cashSessionId,
        UUID cashMovementId,
        UUID paymentReversalCashSessionId,
        UUID paymentReversalCashMovementId,
        Instant confirmedAt,
        UUID confirmedBy,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancellationReason,
        long version,
        Instant updatedAt,
        List<SaleItemResponse> items
    ) {
        static SaleResponse from(Sale sale) {
            return new SaleResponse(sale.id(), sale.branchId(), sale.customerId(), sale.saleNumber(), sale.numberCode(), sale.status(),
                sale.currencyCode(), sale.subtotalAmount(), sale.totalAmount(), sale.paymentStatus(), sale.paymentMethod(), sale.paidAmount(),
                sale.cashSessionId(), sale.cashMovementId(), sale.paymentReversalCashSessionId(), sale.paymentReversalCashMovementId(),
                sale.confirmedAt(), sale.confirmedBy(), sale.cancelledAt(), sale.cancelledBy(), sale.cancellationReason(), sale.version(),
                sale.updatedAt(), sale.items().stream().map(SaleItemResponse::from).toList());
        }
    }

    public record SaleItemResponse(
        UUID id,
        int lineNumber,
        UUID productId,
        UUID warehouseId,
        String productSku,
        String productName,
        String productType,
        boolean stockTrackingEnabled,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotalAmount,
        UUID stockMovementId,
        UUID stockReversalMovementId
    ) {
        static SaleItemResponse from(SaleItem item) {
            return new SaleItemResponse(item.id(), item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(), item.productName(),
                item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitPrice(), item.subtotalAmount(), item.stockMovementId(),
                item.stockReversalMovementId());
        }
    }
}