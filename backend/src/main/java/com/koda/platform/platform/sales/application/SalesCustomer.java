package com.koda.platform.platform.sales.application;

import java.util.UUID;

public record SalesCustomer(UUID id, String legalName, String status, boolean system) {
}