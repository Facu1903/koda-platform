package com.koda.platform.platform.purchases.application;

import java.util.UUID;

public record PurchaseSupplier(UUID id, String legalName, String status, boolean system) {
}