package com.koda.platform.shared.infrastructure.tenant;

import com.koda.platform.shared.application.tenant.TenantContext;
import java.util.Optional;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}