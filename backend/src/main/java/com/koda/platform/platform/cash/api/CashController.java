package com.koda.platform.platform.cash.api;

import com.koda.platform.platform.cash.application.CashMovement;
import com.koda.platform.platform.cash.application.CashRegister;
import com.koda.platform.platform.cash.application.CashRequestMetadata;
import com.koda.platform.platform.cash.application.CashService;
import com.koda.platform.platform.cash.application.CashSession;
import com.koda.platform.platform.cash.application.CloseCashSessionCommand;
import com.koda.platform.platform.cash.application.CreateCashMovementCommand;
import com.koda.platform.platform.cash.application.OpenCashSessionCommand;
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
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/cash")
public class CashController {

    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @GetMapping("/registers")
    public List<CashRegisterResponse> listRegisters() {
        return cashService.listRegisters().stream().map(CashRegisterResponse::from).toList();
    }

    @GetMapping("/sessions")
    public List<CashSessionResponse> listSessions(@RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return cashService.listSessions(limit).stream().map(CashSessionResponse::from).toList();
    }

    @GetMapping("/sessions/current")
    public ResponseEntity<CashSessionResponse> currentSession() {
        return cashService.currentSession()
            .map(session -> ResponseEntity.ok(CashSessionResponse.from(session)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CashSessionResponse openSession(@Valid @RequestBody OpenCashSessionRequest request, HttpServletRequest httpRequest) {
        return CashSessionResponse.from(cashService.openSession(request.toCommand(), metadata(httpRequest)));
    }

    @PostMapping("/sessions/{id}/close")
    public CashSessionResponse closeSession(@PathVariable UUID id, @Valid @RequestBody CloseCashSessionRequest request,
                                            HttpServletRequest httpRequest) {
        return CashSessionResponse.from(cashService.closeSession(id, request.toCommand(), metadata(httpRequest)));
    }

    @GetMapping("/movements")
    public List<CashMovementResponse> listMovements(
        @RequestParam UUID sessionId,
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return cashService.listMovements(sessionId, limit).stream().map(CashMovementResponse::from).toList();
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public CashMovementResponse createMovement(@Valid @RequestBody CashMovementRequest request, HttpServletRequest httpRequest) {
        return CashMovementResponse.from(cashService.createMovement(request.toCommand(), metadata(httpRequest)));
    }

    private CashRequestMetadata metadata(HttpServletRequest request) {
        return new CashRequestMetadata(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }

    public record OpenCashSessionRequest(
        @NotNull UUID cashRegisterId,
        @DecimalMin(value = "0.0000", inclusive = true) BigDecimal openingAmount,
        @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode
    ) {
        OpenCashSessionCommand toCommand() {
            return new OpenCashSessionCommand(cashRegisterId, openingAmount, currencyCode);
        }
    }

    public record CloseCashSessionRequest(
        @Min(0) long version,
        @NotNull @DecimalMin(value = "0.0000", inclusive = true) BigDecimal countedClosingAmount
    ) {
        CloseCashSessionCommand toCommand() {
            return new CloseCashSessionCommand(version, countedClosingAmount);
        }
    }

    public record CashMovementRequest(
        @NotNull UUID cashSessionId,
        @NotBlank @Size(max = 32) String movementType,
        @Size(max = 32) String paymentMethod,
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode,
        @Size(max = 80) String referenceType,
        UUID referenceId,
        @Size(max = 500) String description
    ) {
        CreateCashMovementCommand toCommand() {
            return new CreateCashMovementCommand(cashSessionId, movementType, paymentMethod, amount, currencyCode, referenceType, referenceId,
                description);
        }
    }

    public record CashRegisterResponse(
        UUID id,
        UUID branchId,
        String code,
        String name,
        String status,
        long version,
        Instant updatedAt
    ) {
        static CashRegisterResponse from(CashRegister cashRegister) {
            return new CashRegisterResponse(cashRegister.id(), cashRegister.branchId(), cashRegister.code(), cashRegister.name(),
                cashRegister.status(), cashRegister.version(), cashRegister.updatedAt());
        }
    }

    public record CashSessionResponse(
        UUID id,
        UUID cashRegisterId,
        UUID branchId,
        UUID openedByUserId,
        String status,
        BigDecimal openingAmount,
        String currencyCode,
        BigDecimal expectedClosingAmount,
        BigDecimal countedClosingAmount,
        BigDecimal closingDifference,
        Instant openedAt,
        Instant closedAt,
        long version,
        Instant updatedAt
    ) {
        static CashSessionResponse from(CashSession session) {
            return new CashSessionResponse(session.id(), session.cashRegisterId(), session.branchId(), session.openedByUserId(), session.status(),
                session.openingAmount(), session.currencyCode(), session.expectedClosingAmount(), session.countedClosingAmount(),
                session.closingDifference(), session.openedAt(), session.closedAt(), session.version(), session.updatedAt());
        }
    }

    public record CashMovementResponse(
        UUID id,
        UUID cashSessionId,
        UUID cashRegisterId,
        UUID branchId,
        String movementType,
        String paymentMethod,
        BigDecimal amount,
        BigDecimal cashEffect,
        String currencyCode,
        String referenceType,
        UUID referenceId,
        String description,
        UUID createdByUserId,
        Instant occurredAt
    ) {
        static CashMovementResponse from(CashMovement movement) {
            return new CashMovementResponse(movement.id(), movement.cashSessionId(), movement.cashRegisterId(), movement.branchId(),
                movement.movementType(), movement.paymentMethod(), movement.amount(), movement.cashEffect(), movement.currencyCode(),
                movement.referenceType(), movement.referenceId(), movement.description(), movement.createdByUserId(), movement.occurredAt());
        }
    }
}