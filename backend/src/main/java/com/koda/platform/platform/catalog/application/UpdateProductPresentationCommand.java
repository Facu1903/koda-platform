package com.koda.platform.platform.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductPresentationCommand(long version, UUID unitId, String code, String name, BigDecimal quantity, Boolean active) {
}
