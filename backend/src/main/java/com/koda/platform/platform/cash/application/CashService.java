package com.koda.platform.platform.cash.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashService {

    private static final String ACTIVE = "ACTIVE";
    private static final String OPEN = "OPEN";
    private static final String CASH = "CASH";
    private static final String CASH_IN = "CASH_IN";
    private static final String CASH_OUT = "CASH_OUT";
    private static final String OPENING = "OPENING";
    private static final String CLOSING_ADJUSTMENT = "CLOSING_ADJUSTMENT";
    private static final int MONEY_SCALE = 4;
    private static final int MAX_LIMIT = 500;
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "BANK_TRANSFER", "OTHER");
    private static final Set<String> ALL_ACCESS_ROLES = Set.of("TENANT_OWNER", "TENANT_ADMIN", "MANAGER");

    private final CashRepository repository;
    private final CurrentTenantProvider currentTenantProvider;

    public CashService(CashRepository repository, CurrentTenantProvider currentTenantProvider) {
        this.repository = repository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Transactional(readOnly = true)
    public List<CashRegister> listRegisters() {
        TenantContext context = requirePermission("cash_registers:read");
        return repository.listRegisters(context.tenantId());
    }

    @Transactional(readOnly = true)
    public List<CashSession> listSessions(int limit) {
        TenantContext context = requirePermission("cash_sessions:read");
        UUID openedBy = canReadAllCash(context) ? null : context.userId();
        return repository.listSessions(context.tenantId(), openedBy, normalizeLimit(limit));
    }

    @Transactional(readOnly = true)
    public Optional<CashSession> currentSession() {
        TenantContext context = requirePermission("cash_sessions:read");
        return repository.findCurrentOpenSession(context.tenantId(), context.userId());
    }

    @Transactional
    public CashSession openSession(OpenCashSessionCommand command, CashRequestMetadata metadata) {
        TenantContext context = requirePermission("cash_sessions:open");
        if (command == null) {
            throw new IllegalArgumentException("Cash session open request is required");
        }
        UUID cashRegisterId = required(command.cashRegisterId(), "Cash register");
        CashRegister cashRegister = repository.findActiveRegister(context.tenantId(), cashRegisterId)
            .orElseThrow(() -> new CashItemNotFoundException("cash_register"));
        if (!ACTIVE.equals(cashRegister.status())) {
            throw new CashOperationRejectedException("CASH_REGISTER_INACTIVE", "Cash register is inactive");
        }
        if (repository.existsOpenSession(context.tenantId(), cashRegister.id(), context.userId())) {
            throw new CashOperationRejectedException("CASH_SESSION_ALREADY_OPEN", "Cash session is already open for this register and user");
        }

        String currencyCode = currencyOrTenantDefault(context.tenantId(), command.currencyCode());
        BigDecimal openingAmount = moneyOrZero(command.openingAmount(), "Opening amount");
        OpenCashSessionCommand normalized = new OpenCashSessionCommand(cashRegister.id(), openingAmount, currencyCode);
        CashSession session = repository.createSession(context.tenantId(), context.userId(), cashRegister, normalized);
        repository.createMovement(context.tenantId(), context.userId(), session,
            new CreateCashMovementCommand(session.id(), OPENING, CASH, openingAmount, currencyCode, null, null, "Opening balance"), openingAmount);

        audit(context, "cash.session.open", "cash_session", session.id(), metadata,
            details("cashRegisterId", session.cashRegisterId(), "openingAmount", session.openingAmount(), "currencyCode", session.currencyCode()));
        return session;
    }

    @Transactional
    public CashSession closeSession(UUID id, CloseCashSessionCommand command, CashRequestMetadata metadata) {
        TenantContext context = requirePermission("cash_sessions:close");
        CloseCashSessionCommand normalized = normalizeClose(command);
        CashSession session = repository.findSessionById(context.tenantId(), required(id, "Cash session"))
            .orElseThrow(() -> new CashItemNotFoundException("cash_session"));
        ensureOpen(session);
        ensureCanOperateSession(context, session);

        BigDecimal expected = money(repository.sumCashEffect(context.tenantId(), session.id(), session.currencyCode()), "Expected closing amount");
        BigDecimal difference = normalized.countedClosingAmount().subtract(expected).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        if (difference.signum() != 0) {
            repository.createMovement(context.tenantId(), context.userId(), session,
                new CreateCashMovementCommand(session.id(), CLOSING_ADJUSTMENT, CASH, difference.abs(), session.currencyCode(), null, null,
                    "Closing adjustment"), difference);
        }

        CashSession closed = repository.closeSession(context.tenantId(), session.id(), context.userId(), normalized, expected, difference)
            .orElseThrow(() -> new CashVersionConflictException("cash_session"));
        audit(context, "cash.session.close", "cash_session", closed.id(), metadata,
            details("expectedClosingAmount", expected, "countedClosingAmount", closed.countedClosingAmount(), "closingDifference", difference));
        return closed;
    }

    @Transactional(readOnly = true)
    public List<CashMovement> listMovements(UUID cashSessionId, int limit) {
        TenantContext context = requirePermission("cash_movements:read");
        CashSession session = repository.findSessionById(context.tenantId(), required(cashSessionId, "Cash session"))
            .orElseThrow(() -> new CashItemNotFoundException("cash_session"));
        ensureCanReadSession(context, session);
        return repository.listMovements(context.tenantId(), session.id(), normalizeLimit(limit));
    }

    @Transactional
    public CashMovement createMovement(CreateCashMovementCommand command, CashRequestMetadata metadata) {
        TenantContext context = requirePermission("cash_movements:create");
        CreateCashMovementCommand normalized = normalizeManualMovement(command);
        CashSession session = repository.findSessionById(context.tenantId(), normalized.cashSessionId())
            .orElseThrow(() -> new CashItemNotFoundException("cash_session"));
        ensureOpen(session);
        ensureCanOperateSession(context, session);
        if (!session.currencyCode().equals(normalized.currencyCode())) {
            throw new CashOperationRejectedException("CURRENCY_MISMATCH", "Cash movement currency must match session currency");
        }
        BigDecimal cashEffect = cashEffect(normalized.movementType(), normalized.paymentMethod(), normalized.amount());
        CashMovement movement = repository.createMovement(context.tenantId(), context.userId(), session, normalized, cashEffect);
        audit(context, "cash.movement.create", "cash_movement", movement.id(), metadata,
            details("cashSessionId", movement.cashSessionId(), "movementType", movement.movementType(), "paymentMethod", movement.paymentMethod(),
                "amount", movement.amount(), "cashEffect", movement.cashEffect()));
        return movement;
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return context;
        }
        throw new PermissionDeniedException(permission);
    }

    private CloseCashSessionCommand normalizeClose(CloseCashSessionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Cash session close request is required");
        }
        requireVersion(command.version());
        return new CloseCashSessionCommand(command.version(), moneyNonNegative(command.countedClosingAmount(), "Counted closing amount"));
    }

    private CreateCashMovementCommand normalizeManualMovement(CreateCashMovementCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Cash movement request is required");
        }
        UUID cashSessionId = required(command.cashSessionId(), "Cash session");
        String movementType = upperRequired(command.movementType(), "Movement type");
        if (!movementType.equals(CASH_IN) && !movementType.equals(CASH_OUT)) {
            throw new CashOperationRejectedException("UNSUPPORTED_MANUAL_MOVEMENT_TYPE", "Manual cash movements must be CASH_IN or CASH_OUT");
        }
        String paymentMethod = command.paymentMethod() == null ? CASH : upperRequired(command.paymentMethod(), "Payment method");
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Payment method must be CASH, CARD, BANK_TRANSFER or OTHER");
        }
        BigDecimal amount = moneyPositive(command.amount(), "Amount");
        String currencyCode = currencyCode(command.currencyCode());
        String referenceType = upperTrimToNull(command.referenceType());
        UUID referenceId = command.referenceId();
        if ((referenceType == null && referenceId != null) || (referenceType != null && referenceId == null)) {
            throw new IllegalArgumentException("Reference type and reference id must be provided together");
        }
        return new CreateCashMovementCommand(cashSessionId, movementType, paymentMethod, amount, currencyCode, referenceType, referenceId,
            trimToNull(command.description()));
    }

    private BigDecimal cashEffect(String movementType, String paymentMethod, BigDecimal amount) {
        if (!CASH.equals(paymentMethod)) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        }
        if (CASH_IN.equals(movementType)) {
            return amount;
        }
        if (CASH_OUT.equals(movementType)) {
            return amount.negate().setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        }
        throw new IllegalArgumentException("Unsupported cash movement type");
    }

    private void ensureOpen(CashSession session) {
        if (!OPEN.equals(session.status())) {
            throw new CashOperationRejectedException("CASH_SESSION_CLOSED", "Cash session is closed");
        }
    }

    private void ensureCanOperateSession(TenantContext context, CashSession session) {
        if (canManageAllCash(context) || session.openedByUserId().equals(context.userId())) {
            return;
        }
        throw new PermissionDeniedException("cash_session:own");
    }

    private void ensureCanReadSession(TenantContext context, CashSession session) {
        if (canReadAllCash(context) || session.openedByUserId().equals(context.userId())) {
            return;
        }
        throw new PermissionDeniedException("cash_session:own");
    }

    private boolean canManageAllCash(TenantContext context) {
        return context.platformAdmin() || context.roles().stream().anyMatch(ALL_ACCESS_ROLES::contains);
    }

    private boolean canReadAllCash(TenantContext context) {
        return canManageAllCash(context) || context.roles().contains("READ_ONLY");
    }

    private String currencyOrTenantDefault(TenantId tenantId, String value) {
        String normalized = upperTrimToNull(value);
        if (normalized == null) {
            normalized = repository.findTenantCurrency(tenantId).orElseThrow(() -> new CashItemNotFoundException("tenant_currency"));
        }
        return currencyCode(normalized);
    }

    private String currencyCode(String value) {
        String normalized = upperRequired(value, "Currency code");
        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency code must use ISO 4217 format");
        }
        return normalized;
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private void requireVersion(long version) {
        if (version < 0) {
            throw new IllegalArgumentException("Version is required");
        }
    }

    private <T> T required(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private String upperRequired(String value, String fieldName) {
        String normalized = upperTrimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private BigDecimal moneyOrZero(BigDecimal value, String fieldName) {
        return moneyNonNegative(value == null ? BigDecimal.ZERO : value, fieldName);
    }

    private BigDecimal moneyPositive(BigDecimal value, String fieldName) {
        BigDecimal normalized = moneyNonNegative(value, fieldName);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return normalized;
    }

    private BigDecimal moneyNonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalized = money(required(value, fieldName), fieldName);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return normalized;
    }

    private BigDecimal money(BigDecimal value, String fieldName) {
        BigDecimal stripped = required(value, fieldName).stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > MONEY_SCALE) {
            throw new IllegalArgumentException(fieldName + " supports up to 4 decimals");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private String upperTrimToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> details(Object... values) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            details.put((String) values[i], values[i + 1]);
        }
        return details;
    }

    private void audit(TenantContext context, String action, String resourceType, UUID resourceId, CashRequestMetadata metadata,
                       Map<String, Object> details) {
        repository.recordAuditEvent(context.tenantId(), context.userId(), action, resourceType, resourceId, "SUCCESS", metadata,
            new LinkedHashMap<>(details));
    }
}