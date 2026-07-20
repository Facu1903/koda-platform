package com.koda.platform.platform.sales.application;

import com.koda.platform.shared.application.security.PermissionDeniedException;
import com.koda.platform.shared.application.tenant.CurrentTenantProvider;
import com.koda.platform.shared.application.tenant.TenantContext;
import com.koda.platform.shared.domain.tenant.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesService {

    private static final String DRAFT = "DRAFT";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String PAID = "PAID";
    private static final String CASH = "CASH";
    private static final int MAX_LIMIT = 500;
    private static final int MAX_ITEMS = 200;
    private static final int QUANTITY_SCALE = 6;
    private static final int MONEY_SCALE = 4;
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "BANK_TRANSFER", "OTHER");

    private final SalesRepository repository;
    private final SalesStockPort stockPort;
    private final SalesCashPort cashPort;
    private final CurrentTenantProvider currentTenantProvider;

    public SalesService(SalesRepository repository, SalesStockPort stockPort, SalesCashPort cashPort,
                        CurrentTenantProvider currentTenantProvider) {
        this.repository = repository;
        this.stockPort = stockPort;
        this.cashPort = cashPort;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Transactional(readOnly = true)
    public List<Sale> listSales(int limit) {
        TenantContext context = requirePermission("sales:read");
        return repository.listSales(context.tenantId(), normalizeLimit(limit));
    }

    @Transactional(readOnly = true)
    public Sale getSale(UUID id) {
        TenantContext context = requirePermission("sales:read");
        return repository.findById(context.tenantId(), required(id, "Sale"))
            .orElseThrow(() -> new SaleNotFoundException("sale"));
    }

    @Transactional
    public Sale createSale(CreateSaleCommand command, SalesRequestMetadata metadata) {
        TenantContext context = requirePermission("sales:create");
        PreparedSaleDraft draft = prepareDraft(context.tenantId(), command == null ? null : command.branchId(), command == null ? null : command.customerId(),
            command == null ? null : command.items());
        Sale sale = repository.createDraft(context.tenantId(), context.userId(), draft);
        audit(context, "sales.sale.create", "sale", sale.id(), metadata,
            details("saleNumber", sale.saleNumber(), "numberCode", sale.numberCode(), "totalAmount", sale.totalAmount()));
        return sale;
    }

    @Transactional
    public Sale updateSale(UUID id, UpdateSaleCommand command, SalesRequestMetadata metadata) {
        TenantContext context = requirePermission("sales:update");
        if (command == null) {
            throw new IllegalArgumentException("Sale update request is required");
        }
        requireVersion(command.version());
        Sale sale = repository.findById(context.tenantId(), required(id, "Sale"))
            .orElseThrow(() -> new SaleNotFoundException("sale"));
        ensureDraft(sale);
        PreparedSaleDraft draft = prepareDraft(context.tenantId(), command.branchId(), command.customerId(), command.items());
        Sale updated = repository.replaceDraft(context.tenantId(), sale.id(), context.userId(), command.version(), draft)
            .orElseThrow(() -> new SaleVersionConflictException("sale"));
        audit(context, "sales.sale.update", "sale", updated.id(), metadata,
            details("saleNumber", updated.saleNumber(), "totalAmount", updated.totalAmount()));
        return updated;
    }

    @Transactional
    public void deleteSale(UUID id, long version, SalesRequestMetadata metadata) {
        TenantContext context = requirePermission("sales:delete");
        requireVersion(version);
        Sale sale = repository.findById(context.tenantId(), required(id, "Sale"))
            .orElseThrow(() -> new SaleNotFoundException("sale"));
        ensureDraft(sale);
        if (!repository.softDeleteDraft(context.tenantId(), sale.id(), context.userId(), version)) {
            throw new SaleVersionConflictException("sale");
        }
        audit(context, "sales.sale.delete", "sale", sale.id(), metadata, details("saleNumber", sale.saleNumber()));
    }

    @Transactional
    public Sale confirmSale(UUID id, ConfirmSaleCommand command, SalesRequestMetadata metadata) {
        TenantContext context = requirePermission("sales:confirm");
        ConfirmSaleCommand normalized = normalizeConfirm(command);
        Sale sale = repository.findById(context.tenantId(), required(id, "Sale"))
            .orElseThrow(() -> new SaleNotFoundException("sale"));
        ensureDraft(sale);
        ensureVersion(sale, normalized.version());

        List<SaleItemStockUpdate> stockUpdates = new ArrayList<>();
        for (SaleItem item : sale.items()) {
            if (item.stockTrackingEnabled()) {
                SalesStockMovement movement = stockPort.issueSaleStock(context, item.warehouseId(), item.productId(), item.quantity(), sale.id(), item.id(),
                    metadata);
                stockUpdates.add(new SaleItemStockUpdate(item.id(), movement.movementId()));
            }
        }

        SalePaymentUpdate paymentUpdate = null;
        if (normalized.cashSessionId() != null) {
            requireContextPermission(context, "cash_movements:create");
            if (sale.totalAmount().signum() <= 0) {
                throw new SaleOperationRejectedException("ZERO_TOTAL_PAYMENT_NOT_ALLOWED", "Zero total sale cannot register a payment");
            }
            SalesCashMovement movement = cashPort.recordSalePayment(context, normalized.cashSessionId(), normalized.paymentMethod(), sale.totalAmount(),
                sale.currencyCode(), sale.id(), metadata);
            paymentUpdate = new SalePaymentUpdate(normalized.cashSessionId(), normalized.paymentMethod(), sale.totalAmount(), movement.movementId());
        }

        Sale confirmed = repository.confirmSale(context.tenantId(), sale.id(), context.userId(), normalized.version(), stockUpdates, paymentUpdate)
            .orElseThrow(() -> new SaleVersionConflictException("sale"));
        audit(context, "sales.sale.confirm", "sale", confirmed.id(), metadata,
            details("saleNumber", confirmed.saleNumber(), "totalAmount", confirmed.totalAmount(), "paymentStatus", confirmed.paymentStatus()));
        return confirmed;
    }

    @Transactional
    public Sale cancelSale(UUID id, CancelSaleCommand command, SalesRequestMetadata metadata) {
        TenantContext context = requirePermission("sales:cancel");
        CancelSaleCommand normalized = normalizeCancel(command);
        Sale sale = repository.findById(context.tenantId(), required(id, "Sale"))
            .orElseThrow(() -> new SaleNotFoundException("sale"));
        ensureConfirmed(sale);
        ensureVersion(sale, normalized.version());

        List<SaleItemStockReversal> stockReversals = new ArrayList<>();
        for (SaleItem item : sale.items()) {
            if (item.stockMovementId() != null) {
                SalesStockMovement movement = stockPort.reverseSaleStock(context, item.warehouseId(), item.productId(), item.quantity(), sale.id(), item.id(),
                    metadata);
                stockReversals.add(new SaleItemStockReversal(item.id(), movement.movementId()));
            }
        }

        SalePaymentReversalUpdate paymentReversal = null;
        if (PAID.equals(sale.paymentStatus())) {
            requireContextPermission(context, "cash_movements:create");
            UUID cashSessionId = required(normalized.cashSessionId(), "Cash session");
            SalesCashMovement movement = cashPort.reverseSalePayment(context, cashSessionId, sale.paymentMethod(), sale.paidAmount(), sale.currencyCode(),
                sale.id(), metadata);
            paymentReversal = new SalePaymentReversalUpdate(cashSessionId, movement.movementId());
        }

        Sale cancelled = repository.cancelSale(context.tenantId(), sale.id(), context.userId(), normalized.version(), normalized.reason(), stockReversals,
            paymentReversal).orElseThrow(() -> new SaleVersionConflictException("sale"));
        audit(context, "sales.sale.cancel", "sale", cancelled.id(), metadata,
            details("saleNumber", cancelled.saleNumber(), "reason", cancelled.cancellationReason(), "paymentStatus", cancelled.paymentStatus()));
        return cancelled;
    }

    private PreparedSaleDraft prepareDraft(TenantId tenantId, UUID branchId, UUID customerId, List<SaleItemCommand> itemCommands) {
        UUID normalizedBranchId = required(branchId, "Branch");
        if (!repository.existsActiveBranch(tenantId, normalizedBranchId)) {
            throw new SaleReferenceNotFoundException("branch", normalizedBranchId);
        }
        SalesCustomer customer = resolveCustomer(tenantId, customerId);
        String currencyCode = repository.findTenantCurrency(tenantId).orElseThrow(() -> new SaleReferenceNotFoundException("tenant_currency", tenantId.value()));
        currencyCode = currencyCode(currencyCode);
        List<SaleItemCommand> commands = requireItems(itemCommands);
        List<PreparedSaleItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        int lineNumber = 1;
        for (SaleItemCommand command : commands) {
            PreparedSaleItem item = prepareItem(tenantId, normalizedBranchId, lineNumber, command);
            items.add(item);
            total = total.add(item.subtotalAmount()).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
            lineNumber++;
        }
        return new PreparedSaleDraft(normalizedBranchId, customer.id(), currencyCode, total, total, List.copyOf(items));
    }

    private PreparedSaleItem prepareItem(TenantId tenantId, UUID branchId, int lineNumber, SaleItemCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Sale item is required");
        }
        UUID productId = required(command.productId(), "Product");
        SalesProduct product = repository.findSellableProduct(tenantId, productId)
            .orElseThrow(() -> new SaleReferenceNotFoundException("product", productId));
        if (!"ACTIVE".equals(product.status())) {
            throw new SaleOperationRejectedException("PRODUCT_INACTIVE", "Product is inactive");
        }
        if (!"GOOD".equals(product.productType()) && !"SERVICE".equals(product.productType())) {
            throw new SaleOperationRejectedException("PRODUCT_NOT_SELLABLE", "Product is not sellable in Sprint 2");
        }
        if (product.allowNegativeStock()) {
            throw new SaleOperationRejectedException("UNSUPPORTED_NEGATIVE_STOCK_POLICY", "Negative stock policy is not supported in Sprint 2");
        }
        UUID warehouseId = command.warehouseId();
        if (product.stockTrackingEnabled()) {
            warehouseId = required(warehouseId, "Warehouse");
        }
        if (warehouseId != null && !repository.existsActiveWarehouse(tenantId, branchId, warehouseId)) {
            throw new SaleReferenceNotFoundException("warehouse", warehouseId);
        }
        BigDecimal quantity = scaleQuantity(required(command.quantity(), "Quantity"));
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        BigDecimal unitPrice = scaleMoney(required(command.unitPrice(), "Unit price"), "Unit price");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        BigDecimal subtotal = quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new PreparedSaleItem(lineNumber, product.id(), warehouseId, product.sku(), product.name(), product.productType(),
            product.stockTrackingEnabled(), quantity, unitPrice, subtotal);
    }

    private SalesCustomer resolveCustomer(TenantId tenantId, UUID customerId) {
        if (customerId == null) {
            return repository.findDefaultCustomer(tenantId)
                .orElseThrow(() -> new SaleReferenceNotFoundException("default_customer", null));
        }
        return repository.findActiveCustomer(tenantId, customerId)
            .orElseThrow(() -> new SaleReferenceNotFoundException("customer", customerId));
    }

    private List<SaleItemCommand> requireItems(List<SaleItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Sale requires at least one item");
        }
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Sale supports up to 200 items");
        }
        return items;
    }

    private ConfirmSaleCommand normalizeConfirm(ConfirmSaleCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Sale confirm request is required");
        }
        requireVersion(command.version());
        String paymentMethod = upperTrimToNull(command.paymentMethod());
        UUID cashSessionId = command.cashSessionId();
        if ((cashSessionId == null && paymentMethod != null) || (cashSessionId != null && paymentMethod == null)) {
            throw new IllegalArgumentException("Cash session and payment method must be provided together");
        }
        if (paymentMethod != null && !PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Payment method must be CASH, CARD, BANK_TRANSFER or OTHER");
        }
        return new ConfirmSaleCommand(command.version(), cashSessionId, paymentMethod);
    }

    private CancelSaleCommand normalizeCancel(CancelSaleCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Sale cancel request is required");
        }
        requireVersion(command.version());
        return new CancelSaleCommand(command.version(), trimToNull(command.reason()), command.cashSessionId());
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        requireContextPermission(context, permission);
        return context;
    }

    private void requireContextPermission(TenantContext context, String permission) {
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return;
        }
        throw new PermissionDeniedException(permission);
    }

    private void ensureDraft(Sale sale) {
        if (!DRAFT.equals(sale.status())) {
            throw new SaleOperationRejectedException("SALE_NOT_DRAFT", "Sale is not draft");
        }
    }

    private void ensureConfirmed(Sale sale) {
        if (!CONFIRMED.equals(sale.status())) {
            throw new SaleOperationRejectedException("SALE_NOT_CONFIRMED", "Sale is not confirmed");
        }
    }

    private void ensureVersion(Sale sale, long version) {
        requireVersion(version);
        if (sale.version() != version) {
            throw new SaleVersionConflictException("sale");
        }
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

    private BigDecimal scaleQuantity(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > QUANTITY_SCALE) {
            throw new IllegalArgumentException("Quantity supports up to 6 decimals");
        }
        return value.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    private BigDecimal scaleMoney(BigDecimal value, String fieldName) {
        BigDecimal stripped = value.stripTrailingZeros();
        int effectiveScale = Math.max(0, stripped.scale());
        if (effectiveScale > MONEY_SCALE) {
            throw new IllegalArgumentException(fieldName + " supports up to 4 decimals");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private String currencyCode(String value) {
        String normalized = upperTrimToNull(value);
        if (normalized == null || !normalized.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency code must use ISO 4217 format");
        }
        return normalized;
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

    private void audit(TenantContext context, String action, String resourceType, UUID resourceId, SalesRequestMetadata metadata,
                       Map<String, Object> details) {
        repository.recordAuditEvent(context.tenantId(), context.userId(), action, resourceType, resourceId, "SUCCESS", metadata,
            new LinkedHashMap<>(details));
    }
}