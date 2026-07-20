package com.koda.platform.platform.sales.infrastructure;

import com.koda.platform.platform.cash.application.CashMovement;
import com.koda.platform.platform.cash.application.CashRepository;
import com.koda.platform.platform.cash.application.CashRequestMetadata;
import com.koda.platform.platform.cash.application.CashSession;
import com.koda.platform.platform.cash.application.CreateCashMovementCommand;
import com.koda.platform.platform.sales.application.SaleOperationRejectedException;
import com.koda.platform.platform.sales.application.SaleReferenceNotFoundException;
import com.koda.platform.platform.sales.application.SalesCashMovement;
import com.koda.platform.platform.sales.application.SalesCashPort;
import com.koda.platform.platform.sales.application.SalesRequestMetadata;
import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "koda.sales.jdbc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSalesCashAdapter implements SalesCashPort {

    private static final String CASH = "CASH";
    private static final int MONEY_SCALE = 4;
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "BANK_TRANSFER", "OTHER");
    private static final Set<String> ALL_ACCESS_ROLES = Set.of("TENANT_OWNER", "TENANT_ADMIN", "MANAGER");

    private final CashRepository cashRepository;

    public JdbcSalesCashAdapter(CashRepository cashRepository) {
        this.cashRepository = cashRepository;
    }

    @Override
    public SalesCashMovement recordSalePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount, String currencyCode,
                                               UUID saleId, SalesRequestMetadata metadata) {
        return createSalePaymentMovement(context, cashSessionId, paymentMethod, amount, currencyCode, saleId, false, metadata);
    }

    @Override
    public SalesCashMovement reverseSalePayment(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount, String currencyCode,
                                                UUID saleId, SalesRequestMetadata metadata) {
        return createSalePaymentMovement(context, cashSessionId, paymentMethod, amount, currencyCode, saleId, true, metadata);
    }

    private SalesCashMovement createSalePaymentMovement(TenantContext context, UUID cashSessionId, String paymentMethod, BigDecimal amount,
                                                        String currencyCode, UUID saleId, boolean reversal, SalesRequestMetadata metadata) {
        if (!context.platformAdmin() && !context.hasPermission("cash_movements:create")) {
            throw new PermissionDeniedException("cash_movements:create");
        }
        CashSession session = cashRepository.findSessionById(context.tenantId(), cashSessionId)
            .orElseThrow(() -> new SaleReferenceNotFoundException("cash_session", cashSessionId));
        if (!"OPEN".equals(session.status())) {
            throw new SaleOperationRejectedException("CASH_SESSION_CLOSED", "Cash session is closed");
        }
        if (!canOperateSession(context, session)) {
            throw new PermissionDeniedException("cash_session:own");
        }
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizedCurrency = normalizeCurrency(currencyCode);
        if (!session.currencyCode().equals(normalizedCurrency)) {
            throw new SaleOperationRejectedException("CURRENCY_MISMATCH", "Cash movement currency must match session currency");
        }
        BigDecimal normalizedAmount = moneyPositive(amount);
        BigDecimal cashEffect = CASH.equals(normalizedPaymentMethod) ? normalizedAmount : BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        if (reversal) {
            cashEffect = cashEffect.negate().setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        }
        CreateCashMovementCommand command = new CreateCashMovementCommand(
            session.id(),
            "SALE_PAYMENT",
            normalizedPaymentMethod,
            normalizedAmount,
            normalizedCurrency,
            "SALE",
            saleId,
            reversal ? "Sale payment reversal" : "Sale payment"
        );
        CashMovement movement = cashRepository.createMovement(context.tenantId(), context.userId(), session, command, cashEffect);
        cashRepository.recordAuditEvent(context.tenantId(), context.userId(), "cash.movement.create", "cash_movement", movement.id(), "SUCCESS",
            new CashRequestMetadata(metadata == null ? null : metadata.sourceIp(), metadata == null ? null : metadata.userAgent()),
            details(movement, saleId, reversal));
        return new SalesCashMovement(movement.id());
    }

    private boolean canOperateSession(TenantContext context, CashSession session) {
        return context.platformAdmin() || context.roles().stream().anyMatch(ALL_ACCESS_ROLES::contains) || session.openedByUserId().equals(context.userId());
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = trimToNull(paymentMethod);
        if (normalized == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(normalized)) {
            throw new IllegalArgumentException("Payment method must be CASH, CARD, BANK_TRANSFER or OTHER");
        }
        return normalized;
    }

    private String normalizeCurrency(String currencyCode) {
        String normalized = trimToNull(currencyCode);
        if (normalized == null) {
            throw new IllegalArgumentException("Currency code is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency code must use ISO 4217 format");
        }
        return normalized;
    }

    private BigDecimal moneyPositive(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal stripped = value.stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > MONEY_SCALE) {
            throw new IllegalArgumentException("Amount supports up to 4 decimals");
        }
        BigDecimal normalized = value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> details(CashMovement movement, UUID saleId, boolean reversal) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("cashSessionId", movement.cashSessionId());
        details.put("movementType", movement.movementType());
        details.put("paymentMethod", movement.paymentMethod());
        details.put("amount", movement.amount());
        details.put("cashEffect", movement.cashEffect());
        details.put("saleId", saleId);
        details.put("reversal", reversal);
        return details;
    }
}