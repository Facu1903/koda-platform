package com.koda.platform.platform.purchases.application;

import com.koda.platform.platform.licensing.application.LicensedModules;
import com.koda.platform.platform.licensing.application.LicensedProducts;
import com.koda.platform.platform.licensing.application.TenantLicenseAccessGuard;
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
public class PurchasesService {

    private static final String DRAFT = "DRAFT";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String PAID = "PAID";
    private static final int MAX_LIMIT = 500;
    private static final int MAX_ITEMS = 200;
    private static final int QUANTITY_SCALE = 6;
    private static final int MONEY_SCALE = 4;
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "BANK_TRANSFER", "OTHER");

    private final PurchasesRepository repository;
    private final PurchasesStockPort stockPort;
    private final PurchasesCashPort cashPort;
    private final CurrentTenantProvider currentTenantProvider;
    private final TenantLicenseAccessGuard licenseAccessGuard;

    public PurchasesService(PurchasesRepository repository, PurchasesStockPort stockPort, PurchasesCashPort cashPort,
                            CurrentTenantProvider currentTenantProvider, TenantLicenseAccessGuard licenseAccessGuard) {
        this.repository = repository;
        this.stockPort = stockPort;
        this.cashPort = cashPort;
        this.currentTenantProvider = currentTenantProvider;
        this.licenseAccessGuard = licenseAccessGuard;
    }

    @Transactional(readOnly = true)
    public List<Purchase> listPurchases(int limit) {
        TenantContext context = requirePermission("purchases:read");
        return repository.listPurchases(context.tenantId(), normalizeLimit(limit));
    }

    @Transactional(readOnly = true)
    public Purchase getPurchase(UUID id) {
        TenantContext context = requirePermission("purchases:read");
        return repository.findById(context.tenantId(), required(id, "Purchase"))
            .orElseThrow(() -> new PurchaseNotFoundException("purchase"));
    }

    @Transactional
    public Purchase createPurchase(CreatePurchaseCommand command, PurchasesRequestMetadata metadata) {
        TenantContext context = requirePermission("purchases:create");
        PreparedPurchaseDraft draft = prepareDraft(context.tenantId(), command == null ? null : command.branchId(), command == null ? null : command.supplierId(),
            command == null ? null : command.supplierDocumentNumber(), command == null ? null : command.items());
        Purchase purchase = repository.createDraft(context.tenantId(), context.userId(), draft);
        audit(context, "purchases.purchase.create", "purchase", purchase.id(), metadata,
            details("purchaseNumber", purchase.purchaseNumber(), "numberCode", purchase.numberCode(), "totalAmount", purchase.totalAmount()));
        return purchase;
    }

    @Transactional
    public Purchase updatePurchase(UUID id, UpdatePurchaseCommand command, PurchasesRequestMetadata metadata) {
        TenantContext context = requirePermission("purchases:update");
        if (command == null) {
            throw new IllegalArgumentException("Purchase update request is required");
        }
        requireVersion(command.version());
        Purchase purchase = repository.findById(context.tenantId(), required(id, "Purchase"))
            .orElseThrow(() -> new PurchaseNotFoundException("purchase"));
        ensureDraft(purchase);
        PreparedPurchaseDraft draft = prepareDraft(context.tenantId(), command.branchId(), command.supplierId(), command.supplierDocumentNumber(),
            command.items());
        Purchase updated = repository.replaceDraft(context.tenantId(), purchase.id(), context.userId(), command.version(), draft)
            .orElseThrow(() -> new PurchaseVersionConflictException("purchase"));
        audit(context, "purchases.purchase.update", "purchase", updated.id(), metadata,
            details("purchaseNumber", updated.purchaseNumber(), "totalAmount", updated.totalAmount()));
        return updated;
    }

    @Transactional
    public void deletePurchase(UUID id, long version, PurchasesRequestMetadata metadata) {
        TenantContext context = requirePermission("purchases:delete");
        requireVersion(version);
        Purchase purchase = repository.findById(context.tenantId(), required(id, "Purchase"))
            .orElseThrow(() -> new PurchaseNotFoundException("purchase"));
        ensureDraft(purchase);
        if (!repository.softDeleteDraft(context.tenantId(), purchase.id(), context.userId(), version)) {
            throw new PurchaseVersionConflictException("purchase");
        }
        audit(context, "purchases.purchase.delete", "purchase", purchase.id(), metadata, details("purchaseNumber", purchase.purchaseNumber()));
    }

    @Transactional
    public Purchase confirmPurchase(UUID id, ConfirmPurchaseCommand command, PurchasesRequestMetadata metadata) {
        TenantContext context = requirePermission("purchases:confirm");
        ConfirmPurchaseCommand normalized = normalizeConfirm(command);
        Purchase purchase = repository.findById(context.tenantId(), required(id, "Purchase"))
            .orElseThrow(() -> new PurchaseNotFoundException("purchase"));
        ensureDraft(purchase);
        ensureVersion(purchase, normalized.version());

        List<PurchaseItemStockUpdate> stockUpdates = new ArrayList<>();
        if (purchase.items().stream().anyMatch(PurchaseItem::stockTrackingEnabled)) {
            requireModule(context, LicensedModules.STOCK);
        }
        for (PurchaseItem item : purchase.items()) {
            if (item.stockTrackingEnabled()) {
                PurchasesStockMovement movement = stockPort.receivePurchaseStock(context, item.warehouseId(), item.productId(), item.quantity(),
                    purchase.id(), item.id(), metadata);
                stockUpdates.add(new PurchaseItemStockUpdate(item.id(), movement.movementId()));
            }
        }

        PurchasePaymentUpdate paymentUpdate = null;
        if (normalized.cashSessionId() != null) {
            requireModule(context, LicensedModules.CASH);
            requireContextPermission(context, "cash_movements:create");
            if (purchase.totalAmount().signum() <= 0) {
                throw new PurchaseOperationRejectedException("ZERO_TOTAL_PAYMENT_NOT_ALLOWED", "Zero total purchase cannot register a payment");
            }
            PurchasesCashMovement movement = cashPort.recordPurchasePayment(context, normalized.cashSessionId(), normalized.paymentMethod(),
                purchase.totalAmount(), purchase.currencyCode(), purchase.id(), metadata);
            paymentUpdate = new PurchasePaymentUpdate(normalized.cashSessionId(), normalized.paymentMethod(), purchase.totalAmount(), movement.movementId());
        }

        Purchase confirmed = repository.confirmPurchase(context.tenantId(), purchase.id(), context.userId(), normalized.version(), stockUpdates, paymentUpdate)
            .orElseThrow(() -> new PurchaseVersionConflictException("purchase"));
        audit(context, "purchases.purchase.confirm", "purchase", confirmed.id(), metadata,
            details("purchaseNumber", confirmed.purchaseNumber(), "totalAmount", confirmed.totalAmount(), "paymentStatus", confirmed.paymentStatus()));
        return confirmed;
    }

    @Transactional
    public Purchase cancelPurchase(UUID id, CancelPurchaseCommand command, PurchasesRequestMetadata metadata) {
        TenantContext context = requirePermission("purchases:cancel");
        CancelPurchaseCommand normalized = normalizeCancel(command);
        Purchase purchase = repository.findById(context.tenantId(), required(id, "Purchase"))
            .orElseThrow(() -> new PurchaseNotFoundException("purchase"));
        ensureConfirmed(purchase);
        ensureVersion(purchase, normalized.version());

        List<PurchaseItemStockReversal> stockReversals = new ArrayList<>();
        if (purchase.items().stream().anyMatch(item -> item.stockMovementId() != null)) {
            requireModule(context, LicensedModules.STOCK);
        }
        for (PurchaseItem item : purchase.items()) {
            if (item.stockMovementId() != null) {
                PurchasesStockMovement movement = stockPort.reversePurchaseStock(context, item.warehouseId(), item.productId(), item.quantity(),
                    purchase.id(), item.id(), metadata);
                stockReversals.add(new PurchaseItemStockReversal(item.id(), movement.movementId()));
            }
        }

        PurchasePaymentReversalUpdate paymentReversal = null;
        if (PAID.equals(purchase.paymentStatus())) {
            requireModule(context, LicensedModules.CASH);
            requireContextPermission(context, "cash_movements:create");
            UUID cashSessionId = required(normalized.cashSessionId(), "Cash session");
            PurchasesCashMovement movement = cashPort.reversePurchasePayment(context, cashSessionId, purchase.paymentMethod(), purchase.paidAmount(),
                purchase.currencyCode(), purchase.id(), metadata);
            paymentReversal = new PurchasePaymentReversalUpdate(cashSessionId, movement.movementId());
        }

        Purchase cancelled = repository.cancelPurchase(context.tenantId(), purchase.id(), context.userId(), normalized.version(), normalized.reason(),
            stockReversals, paymentReversal).orElseThrow(() -> new PurchaseVersionConflictException("purchase"));
        audit(context, "purchases.purchase.cancel", "purchase", cancelled.id(), metadata,
            details("purchaseNumber", cancelled.purchaseNumber(), "reason", cancelled.cancellationReason(), "paymentStatus", cancelled.paymentStatus()));
        return cancelled;
    }

    private PreparedPurchaseDraft prepareDraft(TenantId tenantId, UUID branchId, UUID supplierId, String supplierDocumentNumber,
                                               List<PurchaseItemCommand> itemCommands) {
        UUID normalizedBranchId = required(branchId, "Branch");
        if (!repository.existsActiveBranch(tenantId, normalizedBranchId)) {
            throw new PurchaseReferenceNotFoundException("branch", normalizedBranchId);
        }
        PurchaseSupplier supplier = resolveSupplier(tenantId, supplierId);
        String currencyCode = repository.findTenantCurrency(tenantId)
            .orElseThrow(() -> new PurchaseReferenceNotFoundException("tenant_currency", tenantId.value()));
        currencyCode = currencyCode(currencyCode);
        List<PurchaseItemCommand> commands = requireItems(itemCommands);
        List<PreparedPurchaseItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        int lineNumber = 1;
        for (PurchaseItemCommand command : commands) {
            PreparedPurchaseItem item = prepareItem(tenantId, normalizedBranchId, lineNumber, command);
            items.add(item);
            total = total.add(item.subtotalAmount()).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
            lineNumber++;
        }
        return new PreparedPurchaseDraft(normalizedBranchId, supplier.id(), trimToNull(supplierDocumentNumber), currencyCode, total, total,
            List.copyOf(items));
    }

    private PreparedPurchaseItem prepareItem(TenantId tenantId, UUID branchId, int lineNumber, PurchaseItemCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Purchase item is required");
        }
        UUID productId = required(command.productId(), "Product");
        PurchaseProduct product = repository.findPurchasableProduct(tenantId, productId)
            .orElseThrow(() -> new PurchaseReferenceNotFoundException("product", productId));
        if (!"ACTIVE".equals(product.status())) {
            throw new PurchaseOperationRejectedException("PRODUCT_INACTIVE", "Product is inactive");
        }
        if (!"GOOD".equals(product.productType())) {
            throw new PurchaseOperationRejectedException("PRODUCT_NOT_PURCHASABLE", "Only GOOD products can be purchased in Sprint 2");
        }
        if (product.allowNegativeStock()) {
            throw new PurchaseOperationRejectedException("UNSUPPORTED_NEGATIVE_STOCK_POLICY", "Negative stock policy is not supported in Sprint 2");
        }
        UUID warehouseId = command.warehouseId();
        if (product.stockTrackingEnabled()) {
            warehouseId = required(warehouseId, "Warehouse");
        }
        if (warehouseId != null && !repository.existsActiveWarehouse(tenantId, branchId, warehouseId)) {
            throw new PurchaseReferenceNotFoundException("warehouse", warehouseId);
        }
        BigDecimal quantity = scaleQuantity(required(command.quantity(), "Quantity"));
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        BigDecimal unitCost = scaleMoney(required(command.unitCost(), "Unit cost"), "Unit cost");
        if (unitCost.signum() < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        BigDecimal subtotal = quantity.multiply(unitCost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new PreparedPurchaseItem(lineNumber, product.id(), warehouseId, product.sku(), product.name(), product.productType(),
            product.stockTrackingEnabled(), quantity, unitCost, subtotal);
    }

    private PurchaseSupplier resolveSupplier(TenantId tenantId, UUID supplierId) {
        UUID id = required(supplierId, "Supplier");
        return repository.findActiveSupplier(tenantId, id)
            .orElseThrow(() -> new PurchaseReferenceNotFoundException("supplier", id));
    }

    private List<PurchaseItemCommand> requireItems(List<PurchaseItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Purchase requires at least one item");
        }
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Purchase supports up to 200 items");
        }
        return items;
    }

    private ConfirmPurchaseCommand normalizeConfirm(ConfirmPurchaseCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Purchase confirm request is required");
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
        return new ConfirmPurchaseCommand(command.version(), cashSessionId, paymentMethod);
    }

    private CancelPurchaseCommand normalizeCancel(CancelPurchaseCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Purchase cancel request is required");
        }
        requireVersion(command.version());
        return new CancelPurchaseCommand(command.version(), trimToNull(command.reason()), command.cashSessionId());
    }

    private TenantContext requirePermission(String permission) {
        TenantContext context = currentTenantProvider.requireContext();
        requireModule(context, LicensedModules.PURCHASES);
        requireContextPermission(context, permission);
        return context;
    }

    private void requireModule(TenantContext context, String moduleCode) {
        licenseAccessGuard.requireModuleEnabled(context, LicensedProducts.KODA_ERP, moduleCode);
    }

    private void requireContextPermission(TenantContext context, String permission) {
        if (context.platformAdmin() || context.hasPermission(permission)) {
            return;
        }
        throw new PermissionDeniedException(permission);
    }

    private void ensureDraft(Purchase purchase) {
        if (!DRAFT.equals(purchase.status())) {
            throw new PurchaseOperationRejectedException("PURCHASE_NOT_DRAFT", "Purchase is not draft");
        }
    }

    private void ensureConfirmed(Purchase purchase) {
        if (!CONFIRMED.equals(purchase.status())) {
            throw new PurchaseOperationRejectedException("PURCHASE_NOT_CONFIRMED", "Purchase is not confirmed");
        }
    }

    private void ensureVersion(Purchase purchase, long version) {
        requireVersion(version);
        if (purchase.version() != version) {
            throw new PurchaseVersionConflictException("purchase");
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

    private void audit(TenantContext context, String action, String resourceType, UUID resourceId, PurchasesRequestMetadata metadata,
                       Map<String, Object> details) {
        repository.recordAuditEvent(context.tenantId(), context.userId(), action, resourceType, resourceId, "SUCCESS", metadata,
            new LinkedHashMap<>(details));
    }
}