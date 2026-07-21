package com.koda.platform.shared.infrastructure.observability;

public final class LoggingContextKeys {

    public static final String CORRELATION_ID = "correlationId";
    public static final String TENANT_ID = "tenantId";
    public static final String USER_ID = "userId";
    public static final String PLATFORM_ADMIN = "platformAdmin";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String HTTP_PATH = "httpPath";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String HTTP_DURATION_MS = "httpDurationMs";
    public static final String TENANT_ID_REQUEST_ATTRIBUTE = "com.koda.platform.logging.tenantId";
    public static final String USER_ID_REQUEST_ATTRIBUTE = "com.koda.platform.logging.userId";
    public static final String PLATFORM_ADMIN_REQUEST_ATTRIBUTE = "com.koda.platform.logging.platformAdmin";

    private LoggingContextKeys() {
    }
}
