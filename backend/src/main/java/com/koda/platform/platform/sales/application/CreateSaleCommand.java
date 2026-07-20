package com.koda.platform.platform.sales.application;

import java.util.List;
import java.util.UUID;

public record CreateSaleCommand(UUID branchId, UUID customerId, List<SaleItemCommand> items) {
}