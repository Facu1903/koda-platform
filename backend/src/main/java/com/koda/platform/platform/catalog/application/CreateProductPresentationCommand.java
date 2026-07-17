package com.koda.platform.platform.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductPresentationCommand(UUID unitId, String code, String name, BigDecimal quantity, Boolean active) {
}
