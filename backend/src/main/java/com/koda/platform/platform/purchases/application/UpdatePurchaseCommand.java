package com.koda.platform.platform.purchases.application;

import java.util.List;
import java.util.UUID;

public record UpdatePurchaseCommand(long version, UUID branchId, UUID supplierId, String supplierDocumentNumber, List<PurchaseItemCommand> items) {
}