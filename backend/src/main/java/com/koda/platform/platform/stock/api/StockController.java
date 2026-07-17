package com.koda.platform.platform.stock.api;

import com.koda.platform.platform.stock.application.CreateStockMovementCommand;
import com.koda.platform.platform.stock.application.StockBalance;
import com.koda.platform.platform.stock.application.StockMovement;
import com.koda.platform.platform.stock.application.StockRequestMetadata;
import com.koda.platform.platform.stock.application.StockService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/balances")
    public List<StockBalanceResponse> listBalances(
        @RequestParam(required = false) UUID warehouseId,
        @RequestParam(required = false) UUID productId,
        @RequestParam(defaultValue = "200") @Min(1) @Max(500) int limit
    ) {
        return stockService.listBalances(warehouseId, productId, limit).stream().map(StockBalanceResponse::from).toList();
    }

    @GetMapping("/movements")
    public List<StockMovementResponse> listMovements(
        @RequestParam(required = false) UUID warehouseId,
        @RequestParam(required = false) UUID productId,
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return stockService.listMovements(warehouseId, productId, limit).stream().map(StockMovementResponse::from).toList();
    }

    @GetMapping("/movements/{id}")
    public StockMovementResponse getMovement(@PathVariable UUID id) {
        return StockMovementResponse.from(stockService.getMovement(id));
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse createMovement(@Valid @RequestBody StockMovementRequest request, HttpServletRequest httpRequest) {
        return StockMovementResponse.from(stockService.createMovement(request.toCommand(), metadata(httpRequest)));
    }

    private StockRequestMetadata metadata(HttpServletRequest request) {
        return new StockRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record StockMovementRequest(
        @NotNull UUID warehouseId,
        @NotNull UUID productId,
        @NotBlank @Size(max = 32) String movementType,
        @NotNull @DecimalMin(value = "0.000000", inclusive = true) BigDecimal quantity,
        @DecimalMin(value = "0.0000", inclusive = true) BigDecimal unitCost,
        @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode,
        @Size(max = 80) String referenceType,
        UUID referenceId,
        @Size(max = 500) String reason
    ) {
        CreateStockMovementCommand toCommand() {
            return new CreateStockMovementCommand(warehouseId, productId, movementType, quantity, unitCost, currencyCode, referenceType, referenceId, reason);
        }
    }

    public record StockBalanceResponse(
        UUID id,
        UUID warehouseId,
        UUID productId,
        BigDecimal quantityOnHand,
        BigDecimal reservedQuantity,
        long version,
        Instant updatedAt
    ) {
        static StockBalanceResponse from(StockBalance balance) {
            return new StockBalanceResponse(
                balance.id(),
                balance.warehouseId(),
                balance.productId(),
                balance.quantityOnHand(),
                balance.reservedQuantity(),
                balance.version(),
                balance.updatedAt()
            );
        }
    }

    public record StockMovementResponse(
        UUID id,
        UUID warehouseId,
        UUID productId,
        String movementType,
        BigDecimal quantity,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        BigDecimal quantityDelta,
        BigDecimal unitCost,
        String currencyCode,
        String referenceType,
        UUID referenceId,
        UUID reversalOfMovementId,
        String reason,
        Instant confirmedAt,
        UUID confirmedBy
    ) {
        static StockMovementResponse from(StockMovement movement) {
            return new StockMovementResponse(
                movement.id(),
                movement.warehouseId(),
                movement.productId(),
                movement.movementType(),
                movement.quantity(),
                movement.quantityBefore(),
                movement.quantityAfter(),
                movement.quantityDelta(),
                movement.unitCost(),
                movement.currencyCode(),
                movement.referenceType(),
                movement.referenceId(),
                movement.reversalOfMovementId(),
                movement.reason(),
                movement.confirmedAt(),
                movement.confirmedBy()
            );
        }
    }
}