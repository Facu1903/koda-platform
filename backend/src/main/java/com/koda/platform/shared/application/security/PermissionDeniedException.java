package com.koda.platform.shared.application.security;

public class PermissionDeniedException extends RuntimeException {

    private final String requiredPermission;

    public PermissionDeniedException(String requiredPermission) {
        super("Permission denied");
        this.requiredPermission = requiredPermission;
    }

    public String requiredPermission() {
        return requiredPermission;
    }
}