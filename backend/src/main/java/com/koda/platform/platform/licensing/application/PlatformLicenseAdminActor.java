package com.koda.platform.platform.licensing.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PlatformLicenseAdminActor(UUID userId, Set<String> permissions, boolean platformAdmin) {

    public PlatformLicenseAdminActor {
        Objects.requireNonNull(userId, "User id is required");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
