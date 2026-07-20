package com.koda.platform.platform.purchases.api;

import com.koda.platform.platform.purchases.application.CancelPurchaseCommand;
import com.koda.platform.platform.purchases.application.ConfirmPurchaseCommand;
import com.koda.platform.platform.purchases.application.CreatePurchaseCommand;
import com.koda.platform.platform.purchases.application.Purchase;
import com.koda.platform.platform.purchases.application.PurchaseItem;
import com.koda.platform.platform.purchases.application.PurchaseItemCommand;
import com.koda.platform.platform.purchases.application.PurchasesRequestMetadata;
import com.koda.platform.platform.purchases.application.PurchasesService;
import com.koda.platform.platform.purchases.application.UpdatePurchaseCommand;
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
@RequestMapping("/api/v1/purchases")
public class PurchasesController {

    private final PurchasesService purchasesService;

    public PurchasesController(PurchasesService purchasesService) {
        this.purchasesService = purchasesService;
    }

    @GetMapping
    public List<PurchaseResponse> listPurchases(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return purchasesService.listPurchases(limit).stream().map(PurchaseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PurchaseResponse getPurchase(@PathVariable UUID id) {
        return PurchaseResponse.from(purchasesService.getPurchase(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse createPurchase(@Valid @RequestBody PurchaseRequest request, HttpServletRequest httpRequest) {
        return PurchaseResponse.from(purchasesService.createPurchase(request.toCreateCommand(), metadata(httpRequest)));
    }

    @PutMapping("/{id}")
    public PurchaseResponse updatePurchase(@PathVariable UUID id, @Valid @RequestBody VersionedPurchaseRequest request,
                                           HttpServletRequest httpRequest) {
        return PurchaseResponse.from(purchasesService.updatePurchase(id, request.toUpdateCommand(), metadata(httpRequest)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePurchase(@PathVariable UUID id, @RequestParam @Min(0) long version, HttpServletRequest httpRequest) {
        purchasesService.deletePurchase(id, version, metadata(httpRequest));
    }

    @PostMapping("/{id}/confirm")
    public PurchaseResponse confirmPurchase(@PathVariable UUID id, @Valid @RequestBody ConfirmPurchaseRequest request, HttpServletRequest httpRequest) {
        return PurchaseResponse.from(purchasesService.confirmPurchase(id, request.toCommand(), metadata(httpRequest)));
    }

    @PostMapping("/{id}/cancel")
    public PurchaseResponse cancelPurchase(@PathVariable UUID id, @Valid @RequestBody CancelPurchaseRequest request, HttpServletRequest httpRequest) {
        return PurchaseResponse.from(purchasesService.cancelPurchase(id, request.toCommand(), metadata(httpRequest)));
    }

    private PurchasesRequestMetadata metadata(HttpServletRequest request) {
        return new PurchasesRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record PurchaseRequest(
        @NotNull UUID branchId,
        @NotNull UUID supplierId,
        @Size(max = 120) String supplierDocumentNumber,
        @Size(min = 1, max = 200) List<@Valid PurchaseItemRequest> items
    ) {
        CreatePurchaseCommand toCreateCommand() {
            return new CreatePurchaseCommand(branchId, supplierId, supplierDocumentNumber,
                items == null ? null : items.stream().map(PurchaseItemRequest::toCommand).toList());
        }
    }

    public record VersionedPurchaseRequest(
        @Min(0) long version,
        @NotNull UUID branchId,
        @NotNull UUID supplierId,
        @Size(max = 120) String supplierDocumentNumber,
        @Size(min = 1, max = 200) List<@Valid PurchaseItemRequest> items
    ) {
        UpdatePurchaseCommand toUpdateCommand() {
            return new UpdatePurchaseCommand(version, branchId, supplierId, supplierDocumentNumber,
                items == null ? null : items.stream().map(PurchaseItemRequest::toCommand).toList());
        }
    }

    public record PurchaseItemRequest(
        @NotNull UUID productId,
        UUID warehouseId,
        @NotNull @DecimalMin(value = "0.000001", inclusive = true) BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0000", inclusive = true) BigDecimal unitCost
    ) {
        PurchaseItemCommand toCommand() {
            return new PurchaseItemCommand(productId, warehouseId, quantity, unitCost);
        }
    }

    public record ConfirmPurchaseRequest(
        @Min(0) long version,
        UUID cashSessionId,
        @Size(max = 32) String paymentMethod
    ) {
        ConfirmPurchaseCommand toCommand() {
            return new ConfirmPurchaseCommand(version, cashSessionId, paymentMethod);
        }
    }

    public record CancelPurchaseRequest(
        @Min(0) long version,
        @Size(max = 500) String reason,
        UUID cashSessionId
    ) {
        CancelPurchaseCommand toCommand() {
            return new CancelPurchaseCommand(version, reason, cashSessionId);
        }
    }

    public record PurchaseResponse(
        UUID id,
        UUID branchId,
        UUID supplierId,
        long purchaseNumber,
        String numberCode,
        String supplierDocumentNumber,
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
        List<PurchaseItemResponse> items
    ) {
        static PurchaseResponse from(Purchase purchase) {
            return new PurchaseResponse(purchase.id(), purchase.branchId(), purchase.supplierId(), purchase.purchaseNumber(), purchase.numberCode(),
                purchase.supplierDocumentNumber(), purchase.status(), purchase.currencyCode(), purchase.subtotalAmount(), purchase.totalAmount(),
                purchase.paymentStatus(), purchase.paymentMethod(), purchase.paidAmount(), purchase.cashSessionId(), purchase.cashMovementId(),
                purchase.paymentReversalCashSessionId(), purchase.paymentReversalCashMovementId(), purchase.confirmedAt(), purchase.confirmedBy(),
                purchase.cancelledAt(), purchase.cancelledBy(), purchase.cancellationReason(), purchase.version(), purchase.updatedAt(),
                purchase.items().stream().map(PurchaseItemResponse::from).toList());
        }
    }

    public record PurchaseItemResponse(
        UUID id,
        int lineNumber,
        UUID productId,
        UUID warehouseId,
        String productSku,
        String productName,
        String productType,
        boolean stockTrackingEnabled,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal subtotalAmount,
        UUID stockMovementId,
        UUID stockReversalMovementId
    ) {
        static PurchaseItemResponse from(PurchaseItem item) {
            return new PurchaseItemResponse(item.id(), item.lineNumber(), item.productId(), item.warehouseId(), item.productSku(), item.productName(),
                item.productType(), item.stockTrackingEnabled(), item.quantity(), item.unitCost(), item.subtotalAmount(), item.stockMovementId(),
                item.stockReversalMovementId());
        }
    }
}